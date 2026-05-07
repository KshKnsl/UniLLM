import { Bot, User } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/** Strip <think>…</think> blocks emitted by reasoning models (DeepSeek, Qwen, etc.) */
function stripThinkBlocks(text) {
  return text.replace(/<think>[\s\S]*?<\/think>/gi, '').trim();
}

/**
 * ChatMessage
 * @param {{ role: 'user'|'assistant', text: string, model?: string, isError?: boolean }} props
 */
export function ChatMessage({ role, text, model, isError }) {
  const isUser = role === 'user';
  const displayText = isUser ? text : stripThinkBlocks(text);

  return (
    <div className={`flex gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
      {/* Avatar */}
      <div
        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
          isUser
            ? 'bg-emerald-500/30 text-emerald-700'
            : 'bg-cyan-500/30 text-cyan-700'
        }`}
      >
        {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
      </div>

      {/* Bubble */}
      <div
        className={`group relative max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-7 ${
          isUser
            ? 'rounded-tr-sm bg-emerald-500/20 text-slate-900 ring-1 ring-emerald-500/30'
            : isError
            ? 'rounded-tl-sm bg-red-500/15 text-red-800 ring-1 ring-red-500/30'
            : 'rounded-tl-sm bg-slate-200/80 text-slate-900 ring-1 ring-slate-300/60'
        }`}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap break-words">{displayText}</p>
        ) : (
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              p: ({ children }) => <p className="mb-2 last:mb-0 whitespace-pre-wrap break-words">{children}</p>,
              h1: ({ children }) => <h1 className="mb-2 text-base font-bold">{children}</h1>,
              h2: ({ children }) => <h2 className="mb-2 text-sm font-bold">{children}</h2>,
              h3: ({ children }) => <h3 className="mb-1 text-sm font-semibold">{children}</h3>,
              ul: ({ children }) => <ul className="mb-2 ml-4 list-disc space-y-1">{children}</ul>,
              ol: ({ children }) => <ol className="mb-2 ml-4 list-decimal space-y-1">{children}</ol>,
              li: ({ children }) => <li className="leading-relaxed">{children}</li>,
              code: ({ inline, children }) =>
                inline ? (
                  <code className="rounded bg-slate-300/60 px-1 py-0.5 font-mono text-xs text-slate-800">{children}</code>
                ) : (
                  <pre className="my-2 overflow-x-auto rounded-lg bg-slate-800 p-3">
                    <code className="font-mono text-xs text-slate-100">{children}</code>
                  </pre>
                ),
              blockquote: ({ children }) => (
                <blockquote className="my-2 border-l-4 border-slate-400 pl-3 italic text-slate-600">{children}</blockquote>
              ),
              a: ({ href, children }) => (
                <a href={href} target="_blank" rel="noopener noreferrer" className="text-cyan-700 underline hover:text-cyan-900">{children}</a>
              ),
              table: ({ children }) => (
                <div className="my-2 overflow-x-auto">
                  <table className="w-full border-collapse text-xs">{children}</table>
                </div>
              ),
              th: ({ children }) => <th className="border border-slate-300 bg-slate-200 px-2 py-1 text-left font-semibold">{children}</th>,
              td: ({ children }) => <td className="border border-slate-300 px-2 py-1">{children}</td>,
              strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
              em: ({ children }) => <em className="italic">{children}</em>,
              hr: () => <hr className="my-3 border-slate-300" />,
            }}
          >
            {displayText}
          </ReactMarkdown>
        )}
        {!isUser && model && (
          <span className="mt-2 block text-[10px] uppercase tracking-widest text-slate-600">
            {model}
          </span>
        )}
      </div>
    </div>
  );
}
