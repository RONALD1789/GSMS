// Supabase Edge Function: gsms-ai
// Deploy with: supabase functions deploy gsms-ai
// Set the server-side secret with: supabase secrets set GEMINI_API_KEY=...
// JWT verification is enabled by default for deployed Supabase Edge Functions.

const json = (status: number, body: Record<string, unknown>) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") {
    return json(405, { error: "Method not allowed." });
  }

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return json(401, { error: "A valid Supabase session is required." });
  }

  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) {
    return json(503, { error: "The GSMS AI service is not configured by the administrator." });
  }

  let payload: { prompt?: unknown };
  try {
    payload = await request.json();
  } catch {
    return json(400, { error: "Request body must be valid JSON." });
  }

  const prompt = typeof payload.prompt === "string" ? payload.prompt.trim() : "";
  if (!prompt || prompt.length > 24000) {
    return json(400, { error: "Prompt must contain between 1 and 24,000 characters." });
  }

  try {
    const response = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": apiKey,
        },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.35,
            maxOutputTokens: 2048,
          },
        }),
      },
    );

    if (!response.ok) {
      // Do not forward provider response bodies, which may contain operational details.
      return json(502, { error: "The AI provider could not complete the request. Please try again." });
    }

    const result = await response.json();
    const text = result?.candidates?.[0]?.content?.parts
      ?.map((part: { text?: string }) => part.text ?? "")
      .join("")
      .trim();

    if (!text) {
      return json(502, { error: "The AI provider returned an empty response." });
    }

    return json(200, { text });
  } catch {
    return json(502, { error: "The AI service is temporarily unavailable." });
  }
});
