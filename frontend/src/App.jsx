import { useEffect, useMemo, useState } from 'react';

const API_BASE = import.meta.env.VITE_API_URL ?? '';

async function fetchJson(path, options) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers ?? {})
    },
    ...options
  });

  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.message || 'Request failed');
  }
  return payload;
}

export default function App() {
  const [health, setHealth] = useState('checking');
  const [providers, setProviders] = useState([]);
  const [modelsByProvider, setModelsByProvider] = useState({});
  const [selectedProvider, setSelectedProvider] = useState('');
  const [selectedModel, setSelectedModel] = useState('');
  const [prompt, setPrompt] = useState('Explain polymorphism in a practical way.');
  const [responseText, setResponseText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const healthData = await fetchJson('/api/health');
        const providersData = await fetchJson('/api/providers');
        const modelsData = await fetchJson('/api/models');

        if (cancelled) {
          return;
        }

        setHealth(healthData.ok ? 'online' : 'offline');
        setProviders(providersData.providers ?? []);
        setModelsByProvider(modelsData.providers ?? {});

        const firstProvider = providersData.providers?.[0] ?? '';
        const firstModelGroup = modelsData.providers?.[firstProvider] ?? [];
        setSelectedProvider(firstProvider);
        setSelectedModel(firstModelGroup[0] ?? '');
      } catch (err) {
        if (!cancelled) {
          setHealth('offline');
          setError(err.message);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const currentModels = useMemo(() => modelsByProvider[selectedProvider] ?? [], [modelsByProvider, selectedProvider]);

  useEffect(() => {
    if (currentModels.length > 0 && !currentModels.includes(selectedModel)) {
      setSelectedModel(currentModels[0]);
    }
  }, [currentModels, selectedModel]);

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setResponseText('');

    try {
      const result = await fetchJson('/api/chat', {
        method: 'POST',
        body: JSON.stringify({
          model: selectedModel,
          prompt
        })
      });

      setResponseText(result.text ?? '');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="shell">
      <div className="backdrop backdrop-a" />
      <div className="backdrop backdrop-b" />
      <main className="layout">
        <section className="hero card">
          <div className="eyebrow">UniLLM React Console</div>
          <h1>One interface for multiple model providers.</h1>
          <p>
            A focused React frontend that talks to the Java API and lets you browse providers, inspect models,
            and send prompts from one place.
          </p>
          <div className="status-row">
            <span className={`status-dot ${health}`}>{health}</span>
            <span>{providers.length} providers connected</span>
          </div>
        </section>

        <section className="panel card">
          <div className="section-title">Provider map</div>
          <div className="provider-grid">
            {providers.map((provider) => (
              <button
                key={provider}
                className={`provider-chip ${provider === selectedProvider ? 'active' : ''}`}
                onClick={() => {
                  setSelectedProvider(provider);
                  setSelectedModel((modelsByProvider[provider] ?? [])[0] ?? '');
                }}
                type="button"
              >
                {provider}
              </button>
            ))}
          </div>

          <label className="field">
            <span>Model</span>
            <select value={selectedModel} onChange={(event) => setSelectedModel(event.target.value)}>
              {currentModels.length === 0 ? <option value="">No models loaded</option> : null}
              {currentModels.map((model) => (
                <option key={model} value={model}>
                  {model}
                </option>
              ))}
            </select>
          </label>

          <div className="model-list">
            {currentModels.map((model) => (
              <div key={model} className={`model-pill ${model === selectedModel ? 'selected' : ''}`}>
                {model}
              </div>
            ))}
          </div>
        </section>

        <section className="composer card">
          <div className="section-title">Prompt composer</div>
          <form onSubmit={handleSubmit}>
            <label className="field">
              <span>Prompt</span>
              <textarea value={prompt} onChange={(event) => setPrompt(event.target.value)} rows={7} />
            </label>
            <div className="actions">
              <button className="primary-button" type="submit" disabled={loading || !selectedModel}>
                {loading ? 'Running...' : 'Send prompt'}
              </button>
              <button
                className="ghost-button"
                type="button"
                onClick={() => setPrompt('Summarize the current project in three bullet points.')}
              >
                Load sample
              </button>
            </div>
          </form>
        </section>

        <section className="output card">
          <div className="section-title">Response</div>
          <pre>{error ? `Error: ${error}` : responseText || 'The model response will appear here.'}</pre>
        </section>
      </main>
    </div>
  );
}