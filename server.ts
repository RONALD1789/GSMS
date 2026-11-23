import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GoogleGenAI } from '@google/genai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 3000;
const GEMINI_MODEL = 'gemini-4-hydrogen';

function getAiClient(): GoogleGenAI | null {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  if (!apiKey) return null;
  return new GoogleGenAI({
    apiKey,
    httpOptions: { headers: { 'User-Agent': 'aistudio-build' } }
  });
}

function isPlaceholderSecret(value: string): boolean {
  const lower = value.toLowerCase();
  return (
    lower.includes('your_project_ref') ||
    lower.includes('your-project') ||
    lower.includes('your_supabase') ||
    lower.includes('placeholder') ||
    lower.includes('changeme') ||
    lower.includes('example.supabase.co') ||
    lower.includes('<') ||
    lower.includes('>')
  );
}

function getSupabaseServerConfig(): {
  isConfigured: boolean;
  baseUrl: string | null;
  publishableKey: string | null;
  maskedHost: string;
  issue: string | null;
} {
  const rawUrl = process.env.SUPABASE_URL?.trim() || '';
  // Prefer the current publishable key; retain anon-key compatibility for older deployments.
  const rawKey = process.env.SUPABASE_PUBLISHABLE_KEY?.trim() || process.env.SUPABASE_ANON_KEY?.trim() || '';

  if (!rawUrl || !rawKey || isPlaceholderSecret(rawUrl) || isPlaceholderSecret(rawKey)) {
    return {
      isConfigured: false,
      baseUrl: null,
      publishableKey: null,
      maskedHost: 'Local Workspace Mode',
      issue:
        'Supabase cloud credentials are not configured yet. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY secrets to enable cloud sync.'
    };
  }

  let parsed: URL | null = null;
  try {
    parsed = new URL(rawUrl);
  } catch {
    parsed = null;
  }

  if (!parsed || (parsed.protocol !== 'https:' && parsed.protocol !== 'http:')) {
    return {
      isConfigured: false,
      baseUrl: null,
      publishableKey: null,
      maskedHost: 'Invalid URL',
      issue: 'SUPABASE_URL must be a valid https:// URL.'
    };
  }

  const normalizedUrl = rawUrl.replace(/\/+$/, '');
  return {
    isConfigured: true,
    baseUrl: normalizedUrl,
    publishableKey: rawKey,
    maskedHost: parsed.host,
    issue: null
  };
}

async function startServer() {
  const app = express();
  app.use(express.json({ limit: '2mb' }));

  // Server-side Gemini AI Streaming Endpoint
  app.post('/api/ai/stream', async (req, res) => {
    const ai = getAiClient();
    if (!ai) {
      res.status(503).json({ error: 'Gemini AI API key is not configured on the server.' });
      return;
    }

    const { prompt } = req.body as { prompt?: string };
    if (!prompt || typeof prompt !== 'string' || !prompt.trim()) {
      res.status(400).json({ error: 'Prompt is required.' });
      return;
    }

    res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
    res.setHeader('Cache-Control', 'no-cache, no-transform');
    res.setHeader('Connection', 'keep-alive');

    try {
      const responseStream = await ai.models.generateContentStream({
        model: GEMINI_MODEL,
        contents: prompt
      });

      let hasContent = false;
      for await (const chunk of responseStream) {
        const text = chunk.text;
        if (text) {
          hasContent = true;
          res.write(`data: ${JSON.stringify({ text })}\n\n`);
        }
      }
      if (!hasContent) {
        res.write(`data: ${JSON.stringify({ error: 'Gemini returned an empty response.' })}\n\n`);
      }
      res.write('data: [DONE]\n\n');
      res.end();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Gemini streaming failed.';
      res.write(`data: ${JSON.stringify({ error: message })}\n\n`);
      res.end();
    }
  });

  // Server-side Gemini AI Single Generation Endpoint
  app.post('/api/ai/generate', async (req, res) => {
    const ai = getAiClient();
    if (!ai) {
      res.status(503).json({ error: 'Gemini AI API key is not configured on the server.' });
      return;
    }

    const { prompt } = req.body as { prompt?: string };
    if (!prompt || typeof prompt !== 'string' || !prompt.trim()) {
      res.status(400).json({ error: 'Prompt is required.' });
      return;
    }

    try {
      const result = await ai.models.generateContent({
        model: GEMINI_MODEL,
        contents: prompt
      });
      const text = result.text?.trim();
      if (!text) {
        res.status(502).json({ error: 'Gemini returned an empty response.' });
        return;
      }
      res.json({ text });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Gemini generation failed.';
      res.status(500).json({ error: message });
    }
  });

  // Supabase Config Status (keeps the project URL and publishable key server-side)
  app.get('/api/supabase/status', (_req, res) => {
    const cfg = getSupabaseServerConfig();
    res.json({
      isConfigured: cfg.isConfigured,
      endpointLabel: cfg.maskedHost,
      configIssue: cfg.issue
    });
  });

  // Server-side Supabase GoTrue & PostgREST Proxy
  app.all('/api/supabase/proxy/*', async (req, res) => {
    const cfg = getSupabaseServerConfig();
    if (!cfg.isConfigured || !cfg.baseUrl || !cfg.publishableKey) {
      res.status(503).json({ message: cfg.issue || 'Supabase is not configured.' });
      return;
    }

    const rawSuffix = req.path.replace(/^\/api\/supabase\/proxy\/?/, '');
    if (!rawSuffix.startsWith('auth/v1/') && !rawSuffix.startsWith('rest/v1/')) {
      res.status(400).json({ message: 'Unsupported Supabase proxy path.' });
      return;
    }

    const queryIndex = req.originalUrl.indexOf('?');
    const queryString = queryIndex >= 0 ? req.originalUrl.slice(queryIndex) : '';
    const targetUrl = `${cfg.baseUrl}/${rawSuffix}${queryString}`;

    const headers: Record<string, string> = {
      apikey: cfg.publishableKey,
      Accept: 'application/json'
    };

    const incomingAuth = req.header('authorization');
    if (incomingAuth && incomingAuth.startsWith('Bearer ')) {
      headers.Authorization = incomingAuth;
    } else if (cfg.publishableKey.startsWith('eyJ')) {
      // Legacy JWT anon keys also need a bearer header; sb_publishable keys do not.
      headers.Authorization = `Bearer ${cfg.publishableKey}`;
    }

    const prefer = req.header('prefer');
    if (prefer) headers.Prefer = prefer;
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      headers['Content-Type'] = 'application/json';
    }

    try {
      const upstream = await fetch(targetUrl, {
        method: req.method,
        headers,
        body: req.method === 'GET' || req.method === 'HEAD' ? undefined : JSON.stringify(req.body)
      });

      const bodyText = await upstream.text();
      res.status(upstream.status);
      const contentType = upstream.headers.get('content-type');
      if (contentType) res.setHeader('Content-Type', contentType);
      res.send(bodyText);
    } catch (error) {
      const msg = error instanceof Error ? error.message : 'Upstream Supabase error';
      res.status(502).json({
        message: `Unable to reach Supabase host (${cfg.maskedHost}): ${msg}`
      });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer } = await import('vite');
    const vite = await createServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  } else {
    const distDir = path.join(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`GSMS Web Server running at http://0.0.0.0:${PORT}`);
  });
}

startServer().catch((err) => {
  console.error('Failed to start GSMS server:', err);
  process.exit(1);
});
