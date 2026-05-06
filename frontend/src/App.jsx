import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, Bot, Layers3, Loader2, Sparkles, Zap } from 'lucide-react';
import { Badge } from './components/ui/badge';
import { Button } from './components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from './components/ui/card';
import { Label } from './components/ui/label';
import { Separator } from './components/ui/separator';
import { Textarea } from './components/ui/textarea';

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

function formatProviderLabel(provider) {
  return provider.charAt(0).toUpperCase() + provider.slice(1);
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

        const providerList = providersData.providers ?? [];
        const modelsMap = modelsData.providers ?? {};
        const firstProvider = providerList[0] ?? '';
        const firstModelGroup = modelsMap[firstProvider] ?? [];

        setHealth(healthData.ok ? 'online' : 'offline');
        setProviders(providerList);
        setModelsByProvider(modelsMap);
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

  const hasProviders = providers.length > 0;
  const selectedProviderModels = currentModels.length;

  return (
    <div className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,_rgba(34,197,94,0.18),_transparent_30%),radial-gradient(circle_at_top_right,_rgba(14,165,233,0.18),_transparent_28%),linear-gradient(180deg,_#041018_0%,_#081521_45%,_#02060b_100%)] text-foreground">
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_right,rgba(148,163,184,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.08)_1px,transparent_1px)] bg-[size:3rem_3rem] opacity-20" />
      <div className="pointer-events-none absolute -left-32 top-24 h-96 w-96 animate-float rounded-full bg-emerald-400/10 blur-3xl" />
      <div className="pointer-events-none absolute -right-24 top-12 h-[28rem] w-[28rem] animate-float rounded-full bg-cyan-400/10 blur-3xl [animation-delay:1.5s]" />

      <main className="relative mx-auto grid max-w-7xl gap-6 px-4 py-6 md:px-6 lg:grid-cols-[1.1fr_0.9fr] lg:px-8 lg:py-8">
        <Card className="border-border/60 bg-card/80 lg:col-span-2">
          <CardHeader className="space-y-4 p-8">
            <div className="flex flex-wrap items-center gap-3">
              <Badge className="gap-2 bg-emerald-500/15 text-emerald-300 hover:bg-emerald-500/15">
                <Sparkles className="h-3.5 w-3.5" />
                Tailwind + shadcn UI
              </Badge>
              <Badge variant="outline" className="border-border/60 text-slate-200">
                Java API connected through Vite proxy
              </Badge>
            </div>
            <div className="grid gap-3 lg:max-w-4xl">
              <CardTitle className="font-display text-4xl leading-none tracking-tight text-white md:text-6xl">
                One console for multiple model providers.
              </CardTitle>
              <CardDescription className="max-w-3xl text-base leading-7 text-slate-300 md:text-lg">
                A sharper UniLLM frontend built with Tailwind CSS and shadcn-style components. Browse providers,
                inspect model lists, and send prompts to the Java backend from a single interface.
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-3 px-8 pb-8">
            <Badge variant={health === 'online' ? 'default' : 'secondary'} className="gap-2 px-4 py-2 text-sm">
              <Bot className="h-4 w-4" />
              {health === 'online' ? 'API online' : 'API offline'}
            </Badge>
            <Badge variant="outline" className="gap-2 px-4 py-2 text-sm text-slate-200">
              <Layers3 className="h-4 w-4" />
              {providers.length} providers detected
            </Badge>
            <Badge variant="outline" className="gap-2 px-4 py-2 text-sm text-slate-200">
              <Zap className="h-4 w-4" />
              {selectedProviderModels} models in view
            </Badge>
          </CardContent>
        </Card>

        <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-white">
              <Layers3 className="h-5 w-5 text-cyan-300" />
              Providers
            </CardTitle>
            <CardDescription>Choose the provider that should serve the next prompt.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2">
              {hasProviders ? (
                providers.map((provider) => {
                  const active = provider === selectedProvider;
                  return (
                    <Button
                      key={provider}
                      type="button"
                      variant={active ? 'default' : 'outline'}
                      className={[
                        'justify-between rounded-2xl px-4 py-6 text-left text-base shadow-none',
                        active
                          ? 'bg-primary text-primary-foreground'
                          : 'border-border/70 bg-transparent text-slate-100 hover:bg-accent hover:text-accent-foreground'
                      ].join(' ')}
                      onClick={() => {
                        setSelectedProvider(provider);
                        setSelectedModel((modelsByProvider[provider] ?? [])[0] ?? '');
                      }}
                    >
                      <span>{formatProviderLabel(provider)}</span>
                      <span className="text-xs uppercase tracking-[0.24em] text-current/70">{provider}</span>
                    </Button>
                  );
                })
              ) : (
                <div className="rounded-2xl border border-dashed border-border/70 p-4 text-sm text-slate-300">
                  No providers are configured yet.
                </div>
              )}
            </div>

            <Separator className="bg-border/70" />

            <div className="space-y-2">
              <Label htmlFor="model" className="text-slate-200">
                Model
              </Label>
              <select
                id="model"
                value={selectedModel}
                onChange={(event) => setSelectedModel(event.target.value)}
                className="h-12 w-full rounded-2xl border border-border/70 bg-background/80 px-4 text-sm text-foreground outline-none ring-offset-background transition focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                {currentModels.length === 0 ? <option value="">No models loaded</option> : null}
                {currentModels.map((model) => (
                  <option key={model} value={model}>
                    {model}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-wrap gap-2">
              {currentModels.map((model) => (
                <Badge
                  key={model}
                  variant={model === selectedModel ? 'default' : 'secondary'}
                  className="rounded-full px-3 py-1 text-xs"
                >
                  {model}
                </Badge>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-white">
              <ArrowRight className="h-5 w-5 text-emerald-300" />
              Prompt composer
            </CardTitle>
            <CardDescription>Send one prompt to the selected provider and model.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="prompt" className="text-slate-200">
                  Prompt
                </Label>
                <Textarea
                  id="prompt"
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  rows={8}
                  className="min-h-[220px] bg-background/80 text-base leading-7"
                />
              </div>

              <div className="flex flex-wrap gap-3">
                <Button type="submit" disabled={loading || !selectedModel} className="px-5">
                  {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                  {loading ? 'Running request' : 'Send prompt'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  className="border-border/70 bg-transparent text-slate-100 hover:bg-accent hover:text-accent-foreground"
                  onClick={() => setPrompt('Summarize the current project in three bullet points.')}
                >
                  Load sample
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-white">
              <AlertCircle className="h-5 w-5 text-cyan-300" />
              Response
            </CardTitle>
            <CardDescription>Model output and backend errors appear here.</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="min-h-[240px] rounded-2xl border border-border/70 bg-background/70 p-4 text-sm leading-7 text-slate-100">
              {error ? <div className="text-red-300">Error: {error}</div> : responseText || 'The model response will appear here.'}
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}