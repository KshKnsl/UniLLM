import { useEffect, useRef } from 'react';
import { MessageSquare } from 'lucide-react';
import { ChatMessage } from './ChatMessage';

/**
 * ChatWindow
 * @param {{ messages: Array<{id:string, role:string, text:string, model?:string, isError?:boolean}> }} props
 */
export function ChatWindow({ messages }) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center">
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/5 ring-1 ring-white/10">
          <MessageSquare className="h-6 w-6 text-slate-400" />
        </div>
        <p className="text-sm text-slate-400">
          No messages yet. Pick a model and start chatting.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col gap-4 overflow-y-auto pr-1">
      {messages.map((msg) => (
        <ChatMessage
          key={msg.id}
          role={msg.role}
          text={msg.text}
          model={msg.model}
          isError={msg.isError}
        />
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
