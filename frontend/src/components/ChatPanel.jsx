import { useRef, useState } from 'react';
import { Loader2, Send, Trash2 } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { ChatWindow } from './ChatWindow';

function uid() {
  return Math.random().toString(36).slice(2);
}

/**
 * Build fetch headers from saved settings — same pattern as App.jsx.
 */
function buildHeaders(settings) {
  const headers = { 'Content-Type': 'application/json' };
  if (settings.openAiKey?.trim()) headers['x-unillm-openai-key'] = settings.openAiKey.trim();
  if (settings.anthropicKey?.trim()) headers['x-unillm-anthropic-key'] = settings.anthropicKey.trim();
  if (settings.geminiKey?.trim()) headers['x-unillm-gemini-key'] = settings.geminiKey.trim();
  if (settings.groqKey?.trim()) headers['x-unillm-groq-key'] = settings.groqKey.trim();
  headers['x-unillm-include-ollama'] = String(Boolean(settings.includeOllama));
  if (settings.ollamaBaseUrl?.trim()) headers['x-unillm-ollama-base-url'] = settings.ollamaBaseUrl.trim();
  return headers;
}

function apiUrl(baseUrl, path) {
  const base = (baseUrl || '').trim().replace(/\/$/, '');
  return base ? `${base}${path}` : path;
}

/**
 * ChatPanel
 *
 * Self-contained chat UI. Receives settings + selected model from the parent
 * (App.jsx) as props. Manages its own local message history.
 *
 * @param {{
 *   appliedSettings: object,
 *   selectedModel: string,
 *   modelsByProvider: object
 * }} props
 */
export function ChatPanel({ appliedSettings, selectedModel, modelsByProvider }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const textareaRef = useRef(null);

  const allModels = Object.values(modelsByProvider).flat();

  function clearChat() {
    setMessages([]);
    setInput('');
  }

  function handleKeyDown(e) {
    // Ctrl/Cmd + Enter → submit
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      sendMessage();
    }
  }

  async function sendMessage() {
    const text = input.trim();
    if (!text || loading || !selectedModel) return;

    // Append user message
    const userMsg = { id: uid(), role: 'user', text };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const response = await fetch(apiUrl(appliedSettings.apiBaseUrl, '/api/chat'), {
        method: 'POST',
        headers: buildHeaders(appliedSettings),
        body: JSON.stringify({
          model: selectedModel,
          // Send full conversation history so multi-turn context works
          messages: [...messages, userMsg].map((m) => ({
            role: m.role,
            content: m.text
          }))
        })
      });

      const payload = await response.json();

      if (!response.ok) {
        throw new Error(payload.message || 'Request failed');
      }

      setMessages((prev) => [
        ...prev,
        {
          id: uid(),
          role: 'assistant',
          text: payload.text ?? '',
          model: payload.model ?? selectedModel
        }
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { id: uid(), role: 'assistant', text: err.message, isError: true }
      ]);
    } finally {
      setLoading(false);
      // Re-focus textarea after response arrives
      textareaRef.current?.focus();
    }
  }

  const canSend = input.trim().length > 0 && !loading && Boolean(selectedModel);

  return (
    <Card className="flex flex-col border-border/60 bg-card/80 lg:col-span-2" style={{ minHeight: '600px' }}>
      {/* Header */}
      <CardHeader className="shrink-0 pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2 text-xl text-white">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-500/20 text-emerald-300">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                className="h-4 w-4">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
            </span>
            Chat
          </CardTitle>

          <div className="flex items-center gap-2">
            {/* Active model badge */}
            {selectedModel ? (
              <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs text-cyan-300 ring-1 ring-cyan-500/20">
                {selectedModel}
              </span>
            ) : (
              <span className="rounded-full bg-white/5 px-3 py-1 text-xs text-slate-500 ring-1 ring-white/10">
                No model selected
              </span>
            )}

            {/* Clear button */}
            {messages.length > 0 && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={clearChat}
                className="h-8 gap-1.5 text-slate-400 hover:text-red-400"
                title="Clear chat"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Clear
              </Button>
            )}
          </div>
        </div>

        {!selectedModel && (
          <p className="mt-1 text-xs text-amber-400/80">
            ⚠ Save your settings and select a model above to start chatting.
          </p>
        )}
      </CardHeader>

      {/* Messages area */}
      <CardContent className="flex flex-1 flex-col gap-4 overflow-hidden">
        <div className="flex flex-1 flex-col overflow-hidden rounded-2xl border border-border/60 bg-background/60 p-4">
          <ChatWindow messages={messages} />
        </div>

        {/* Input bar */}
        <div className="flex shrink-0 items-end gap-2">
          <div className="relative flex-1">
            <textarea
              ref={textareaRef}
              id="chat-input"
              rows={1}
              value={input}
              onChange={(e) => {
                setInput(e.target.value);
                // Auto-resize
                e.target.style.height = 'auto';
                e.target.style.height = `${Math.min(e.target.scrollHeight, 160)}px`;
              }}
              onKeyDown={handleKeyDown}
              placeholder={
                selectedModel
                  ? `Message ${selectedModel}… (⌘↵ to send)`
                  : 'Select a model first…'
              }
              disabled={!selectedModel || loading}
              className="w-full resize-none overflow-hidden rounded-2xl border border-border/70 bg-background/80 px-4 py-3 text-sm text-foreground outline-none ring-offset-background transition placeholder:text-slate-500 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50"
              style={{ minHeight: '48px' }}
            />
          </div>
          <Button
            id="chat-send-btn"
            type="button"
            disabled={!canSend}
            onClick={sendMessage}
            className="h-12 w-12 shrink-0 rounded-2xl p-0"
          >
            {loading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
