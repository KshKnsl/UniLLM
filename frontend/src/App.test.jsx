import { render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import App from './App';

describe('App', () => {
  const API_BASE_URL = 'https://unillm.onrender.com';

  beforeEach(() => {
    global.fetch = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url;

      if (url.endsWith('/api/health')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ ok: true }) });
      }
      if (url.endsWith('/api/providers')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ providers: ['openai'] }) });
      }
      if (url.endsWith('/api/models')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ providers: { openai: ['gpt-4o'] } }) });
      }
      if (url.endsWith('/api/chat')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ text: 'Hello from UniLLM' }) });
      }

      return Promise.reject(new Error(`Unexpected fetch: ${url}`));
    });
  });

  it('renders the main UI and uses the fixed backend endpoint', async () => {
    render(<App />);

    expect(screen.getByText(/One console for the whole UniLLM library/i)).toBeInTheDocument();
    expect(screen.getByText(/Backend endpoint/i)).toBeInTheDocument();
    expect(screen.getByText(API_BASE_URL)).toBeInTheDocument();

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        `${API_BASE_URL}/api/health`,
        expect.objectContaining({
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          })
        })
      );
    });

    expect(screen.getByRole('heading', { name: /Providers/i })).toBeInTheDocument();
  });
});
