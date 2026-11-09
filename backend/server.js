// Simple Calculator Deign — secure AI backend (Node.js 18+, no dependencies).
//
// The Android app sends only the user's question text here. This server holds the
// AI provider's secret key in an environment variable, so the key is NEVER inside the APK.
//
//   ANTHROPIC_API_KEY=sk-...  PORT=8787  node server.js
//
// Endpoint:  POST /v1/math-assistant   {"question": "...", "history": [{"role":"user|assistant","content":"..."}]}
// Response:  {"answer": "..."}

const http = require("http");

const PORT = Number(process.env.PORT || 8787);
const API_KEY = process.env.ANTHROPIC_API_KEY;
const MODEL = process.env.AI_MODEL || "claude-haiku-5-5";
const MAX_BODY = 32 * 1024;          // bytes
const RATE_LIMIT = 30;               // requests per IP per window
const WINDOW_MS = 10 * 60 * 1000;    // 10 minutes

if (!API_KEY) {
  console.error("Set ANTHROPIC_API_KEY before starting the server.");
  process.exit(1);
}

const SYSTEM_PROMPT = `You are the Math Assistant inside the "Simple Calculator Deign" Android app.
Explain mathematics clearly for beginners. Always:
- show the method step by step, numbering the steps;
- state formulas before using them (percentages, interest, EMI, equations);
- keep arithmetic exact and say when a value is rounded;
- end with a line "Answer: <result>".
Only answer maths, finance-calculation and unit questions. Keep replies under 250 words.`;

const hits = new Map();
function rateLimited(ip) {
  const now = Date.now();
  const entry = hits.get(ip) || { count: 0, start: now };
  if (now - entry.start > WINDOW_MS) { entry.count = 0; entry.start = now; }
  entry.count += 1;
  hits.set(ip, entry);
  return entry.count > RATE_LIMIT;
}

function send(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

async function askModel(question, history) {
  const messages = [];
  for (const turn of history.slice(-10)) {
    if ((turn.role === "user" || turn.role === "assistant") && typeof turn.content === "string" && turn.content.trim()) {
      messages.push({ role: turn.role, content: turn.content.slice(0, 2000) });
    }
  }
  // The API requires alternating roles starting with "user".
  while (messages.length && messages[0].role !== "user") messages.shift();
  const merged = [];
  for (const m of messages) {
    if (merged.length && merged[merged.length - 1].role === m.role) merged[merged.length - 1].content += "\n" + m.content;
    else merged.push({ ...m });
  }
  if (merged.length && merged[merged.length - 1].role === "user") merged.pop();
  merged.push({ role: "user", content: question.slice(0, 2000) });

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": API_KEY,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({ model: MODEL, max_tokens: 800, system: SYSTEM_PROMPT, messages: merged }),
  });
  if (!response.ok) {
    const detail = await response.text();
    console.error("Provider error", response.status, detail.slice(0, 300));
    throw new Error("The AI provider returned an error");
  }
  const data = await response.json();
  return (data.content || []).filter((b) => b.type === "text").map((b) => b.text).join("\n").trim();
}

const server = http.createServer((req, res) => {
  if (req.method === "GET" && req.url === "/health") return send(res, 200, { ok: true });
  if (req.method !== "POST" || req.url !== "/v1/math-assistant") return send(res, 404, { error: "Not found" });

  const ip = req.headers["x-forwarded-for"]?.split(",")[0].trim() || req.socket.remoteAddress || "unknown";
  if (rateLimited(ip)) return send(res, 429, { error: "Too many requests. Please wait a few minutes." });

  let size = 0;
  const chunks = [];
  req.on("data", (chunk) => {
    size += chunk.length;
    if (size > MAX_BODY) { send(res, 413, { error: "Request too large" }); req.destroy(); return; }
    chunks.push(chunk);
  });
  req.on("end", async () => {
    if (res.writableEnded) return;
    let body;
    try { body = JSON.parse(Buffer.concat(chunks).toString("utf8")); } catch { return send(res, 400, { error: "Invalid JSON" }); }
    const question = typeof body.question === "string" ? body.question.trim() : "";
    if (!question) return send(res, 400, { error: "Question is required" });
    const history = Array.isArray(body.history) ? body.history : [];
    try {
      const answer = await askModel(question, history);
      send(res, 200, { answer: answer || "I couldn't produce an answer. Please rephrase the question." });
    } catch (e) {
      send(res, 502, { error: e.message || "AI request failed" });
    }
  });
});

server.listen(PORT, () => console.log(`Math assistant backend listening on :${PORT} (model ${MODEL})`));
