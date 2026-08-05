import { useState, type KeyboardEvent } from 'react';

type ChatInputProps = {
  disabled?: boolean;
  onSend: (content: string) => void;
};

export function ChatInput({ disabled, onSend }: ChatInputProps) {
  const [value, setValue] = useState('');

  function handleSend() {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue('');
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  }

  return (
    <div className="msg-input-bar">
      <textarea
        className="msg-input-bar__textarea"
        placeholder="Nhập tin nhắn..."
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={1}
      />
      <button
        type="button"
        className="msg-input-bar__send"
        onClick={handleSend}
        disabled={disabled || !value.trim()}
      >
        Gửi
      </button>
    </div>
  );
}
