import { Bot, User } from 'lucide-react';

/**
 * ChatMessage
 * @param {{ role: 'user'|'assistant', text: string, model?: string, isError?: boolean }} props
 */
export function ChatMessage({ role, text, model, isError }) {
  const isUser = role === 'user';

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
        <p className="whitespace-pre-wrap break-words">{text}</p>
        {!isUser && model && (
          <span className="mt-2 block text-[10px] uppercase tracking-widest text-slate-600">
            {model}
          </span>
        )}
      </div>
    </div>
  );
}
