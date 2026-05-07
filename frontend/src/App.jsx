import { useEffect, useMemo, useRef, useState } from 'react';
import { ChatPanel } from './components/ChatPanel';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  KeyRound,
  Layers3,
  Loader2,
  RefreshCcw,
  Server,
  Settings2,
  SlidersHorizontal,
  Sparkles,
  Zap
} from 'lucide-react';
import { Badge } from './components/ui/badge';
import { Button } from './components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from './components/ui/dialog';
import { Label } from './components/ui/label';
import { Separator } from './components/ui/separator';
import { Textarea } from './components/ui/textarea';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import { Toaster, toast } from 'sonner';

const SETTINGS_STORAGE_KEY = 'unillm.frontend.settings.v1';
const BACKEND_API_BASE_URL = 'https://unillm.onrender.com';

const DEFAULT_SETTINGS = {
  openAiKey: '',
  anthropicKey: '',
  geminiKey: '',
  groqKey: '',
  includeOllama: false,
  ollamaBaseUrl: 'http://localhost:11434'
};

function loadSettings() {
  if (typeof window === 'undefined') {
    return DEFAULT_SETTINGS;
  }

  const defaults = { ...DEFAULT_SETTINGS };

  try {
    const raw = window.localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (!raw) {
      return defaults;
    }

    return { ...defaults, ...JSON.parse(raw) };
  } catch {
    return defaults;
  }
}

function trimUrl(value) {
  return value.trim().replace(/\/$/, '');
}

function apiUrl(baseUrl, path) {
  const base = trimUrl(baseUrl || '');
  return base ? `${base}${path}` : path;
}

function buildHeaders(settings) {
  const headers = { 'Content-Type': 'application/json' };

  if (settings.openAiKey.trim()) headers['x-unillm-openai-key'] = settings.openAiKey.trim();
  if (settings.anthropicKey.trim()) headers['x-unillm-anthropic-key'] = settings.anthropicKey.trim();
  if (settings.geminiKey.trim()) headers['x-unillm-gemini-key'] = settings.geminiKey.trim();
  if (settings.groqKey.trim()) headers['x-unillm-groq-key'] = settings.groqKey.trim();
  headers['x-unillm-include-ollama'] = String(Boolean(settings.includeOllama));
  if (settings.ollamaBaseUrl.trim()) headers['x-unillm-ollama-base-url'] = settings.ollamaBaseUrl.trim();

  return headers;
}

async function fetchJson(path, settings, options = {}) {
  const response = await fetch(apiUrl(BACKEND_API_BASE_URL, path), {
    ...options,
    headers: {
      ...buildHeaders(settings),
      ...(options.headers ?? {})
    }
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

function SettingsField({ label, value, onChange, type = 'text', placeholder, helpText, autoComplete = 'off' }) {
  return (
    <label className="grid gap-2">
      <span className="text-sm text-slate-700">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        className="h-11 rounded-2xl border border-border/70 bg-background/80 px-4 text-sm text-foreground outline-none ring-offset-background transition placeholder:text-slate-500 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      />
      {helpText ? <span className="text-xs leading-5 text-slate-500">{helpText}</span> : null}
    </label>
  );
}

function App() {
  const [settingsDraft, setSettingsDraft] = useState(loadSettings);
  const [appliedSettings, setAppliedSettings] = useState(loadSettings);
  const [providers, setProviders] = useState([]);
  const settingsRef = useRef(null);
  const [modelsByProvider, setModelsByProvider] = useState({});
  const [selectedProvider, setSelectedProvider] = useState('');
  const [selectedModel, setSelectedModel] = useState('');
  const [prompt, setPrompt] = useState('Explain polymorphism in a practical way.');
  const [responseText, setResponseText] = useState('');
  const [loading, setLoading] = useState(false);
  const [configSaving, setConfigSaving] = useState(false);
  const [error, setError] = useState('');
  

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settingsDraft));
  }, [settingsDraft]);

  useEffect(() => {
    let cancelled = false;

    async function loadLibrary() {
      try {
        await fetchJson('/api/health', appliedSettings);
        const providersData = await fetchJson('/api/providers', appliedSettings);
        const modelsData = await fetchJson('/api/models', appliedSettings);

        if (cancelled) {
          return;
        }

        const providerList = providersData.providers ?? [];
        const modelsMap = modelsData.providers ?? {};
        const nextProvider = providerList.includes(selectedProvider) ? selectedProvider : providerList[0] ?? '';
        const nextModels = modelsMap[nextProvider] ?? [];

        setProviders(providerList);
        setModelsByProvider(modelsMap);
        setSelectedProvider(nextProvider);
        setSelectedModel(nextModels.includes(selectedModel) ? selectedModel : nextModels[0] ?? '');
        setError('');
      } catch (err) {
        if (!cancelled) {
          setError(err.message);
          toast.error(err.message);
        }
      }
    }

    loadLibrary();
    return () => {
      cancelled = true;
    };
  }, [appliedSettings]);

  const currentModels = useMemo(() => modelsByProvider[selectedProvider] ?? [], [modelsByProvider, selectedProvider]);

  useEffect(() => {
    if (currentModels.length > 0 && !currentModels.includes(selectedModel)) {
      setSelectedModel(currentModels[0]);
    }
  }, [currentModels, selectedModel]);

  async function applySettings(event) {
    event.preventDefault();
    setConfigSaving(true);
    setError('');

    try {
      const nextSettings = {
        ...settingsDraft,
        ollamaBaseUrl: trimUrl(settingsDraft.ollamaBaseUrl)
      };

      setSettingsDraft(nextSettings);
      setAppliedSettings(nextSettings);
      toast.success('Settings saved');
    } catch (err) {
      setError(err.message);
      toast.error(err.message);
    } finally {
      setConfigSaving(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setResponseText('');
    const loadingId = toast.loading('Running request');
    try {
      const result = await fetchJson('/api/chat', appliedSettings, {
        method: 'POST',
        body: JSON.stringify({
          model: selectedModel,
          prompt
        })
      });

      setResponseText(result.text ?? '');
      toast.success('Response received', { id: loadingId });
    } catch (err) {
      setError(err.message);
      toast.error(err.message, { id: loadingId });
    } finally {
      setLoading(false);
    }
  }

  const hasProviders = providers.length > 0;
  const selectedProviderModels = currentModels.length;

  return (
    <div className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.12),_transparent_30%),radial-gradient(circle_at_top_right,_rgba(56,189,248,0.14),_transparent_30%),linear-gradient(180deg,_#f8fafc_0%,_#eef6ff_52%,_#f5f7fb_100%)] text-foreground">
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_right,rgba(148,163,184,0.14)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.14)_1px,transparent_1px)] bg-[size:3rem_3rem] opacity-40" />
      <div className="pointer-events-none absolute -left-32 top-24 h-96 w-96 animate-float rounded-full bg-emerald-300/20 blur-3xl" />
      <div className="pointer-events-none absolute -right-24 top-12 h-[28rem] w-[28rem] animate-float rounded-full bg-cyan-300/20 blur-3xl [animation-delay:1.5s]" />

      <main className="relative mx-auto grid max-w-7xl gap-6 px-4 py-4 md:px-6 lg:grid-cols-[1fr_1fr] lg:px-8 lg:py-8">
        <div className="fixed left-0 right-0 top-0 z-40 bg-transparent p-4">
          <div className="mx-auto max-w-7xl flex items-center justify-between gap-4">
            <div className="text-lg font-semibold text-slate-900">UniLLM Console</div>
            <div className="flex items-center gap-2">
              <button
                aria-label="Open settings"
                className="inline-flex items-center gap-2 rounded-lg border border-border/70 bg-white/60 px-3 py-2 text-sm text-slate-800 shadow-sm"
                onClick={() => settingsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })}
              >
                <Settings2 className="h-4 w-4 text-cyan-300" />
                Settings
              </button>
            </div>
          </div>
        </div>
        <div className="h-12" />
        <Card className="border-border/60 bg-card/80 lg:col-span-2">
          <CardHeader className="space-y-4 p-6 sm:p-8">
            <div className="flex flex-wrap items-center gap-3">
              <Badge className="gap-2 bg-emerald-500/15 text-emerald-300 hover:bg-emerald-500/15">
                <Sparkles className="h-3.5 w-3.5" />
                Deployment-ready settings
              </Badge>
            </div>
            <div className="grid gap-3 lg:max-w-4xl">
              <CardTitle className="font-display text-4xl leading-none tracking-tight text-slate-900 md:text-6xl">
                One console for the whole UniLLM library.
              </CardTitle>
              <CardDescription className="max-w-4xl text-base leading-7 text-slate-600 md:text-lg">
                Configure provider credentials, inspect model lists, and send prompts from one control panel.
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-3 p-6 sm:px-8 sm:pb-8">
            <Badge variant="outline" className="gap-2 px-4 py-2 text-sm text-slate-700">
              <Layers3 className="h-4 w-4" />
              {providers.length} providers detected
            </Badge>
            <Badge variant="outline" className="gap-2 px-4 py-2 text-sm text-slate-700">
              <Zap className="h-4 w-4" />
              {selectedProviderModels} models in view
            </Badge>
          </CardContent>
        </Card>

        <Card ref={settingsRef} className="border-border/60 bg-card/80 lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-slate-900">
              <Settings2 className="h-5 w-5 text-cyan-300" />
              Settings
            </CardTitle>
            <CardDescription>
              Manage API keys and runtime options. Configuration is stored in this browser session.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-6" onSubmit={applySettings}>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-border/70 bg-background/50 p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium text-slate-800">Provider keys</p>
                      <p className="text-xs text-slate-400">OpenAI, Anthropic, Gemini, and Groq credentials.</p>
                    </div>
                    <Dialog>
                      <DialogTrigger asChild>
                        <Button type="button" variant="outline" className="border-border/70 bg-transparent text-slate-700">
                          <KeyRound className="h-4 w-4" />
                          Edit keys
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="sm:max-w-xl">
                        <DialogHeader>
                          <DialogTitle>Provider keys</DialogTitle>
                          <DialogDescription>These keys are sent as headers to UniLLM on each request.</DialogDescription>
                        </DialogHeader>
                        <div className="grid gap-4 py-2">
                          <SettingsField
                            label="OpenAI key"
                            type="password"
                            value={settingsDraft.openAiKey}
                            onChange={(value) => setSettingsDraft((current) => ({ ...current, openAiKey: value }))}
                            placeholder="sk-..."
                            autoComplete="off"
                          />
                          <SettingsField
                            label="Anthropic key"
                            type="password"
                            value={settingsDraft.anthropicKey}
                            onChange={(value) => setSettingsDraft((current) => ({ ...current, anthropicKey: value }))}
                            placeholder="sk-ant-..."
                            autoComplete="off"
                          />
                          <SettingsField
                            label="Gemini key"
                            type="password"
                            value={settingsDraft.geminiKey}
                            onChange={(value) => setSettingsDraft((current) => ({ ...current, geminiKey: value }))}
                            placeholder="AIza..."
                            autoComplete="off"
                          />
                          <SettingsField
                            label="Groq key"
                            type="password"
                            value={settingsDraft.groqKey}
                            onChange={(value) => setSettingsDraft((current) => ({ ...current, groqKey: value }))}
                            placeholder="gsk_..."
                            autoComplete="off"
                          />
                        </div>
                        <DialogFooter>
                          <DialogClose asChild>
                            <Button type="button">Done</Button>
                          </DialogClose>
                        </DialogFooter>
                      </DialogContent>
                    </Dialog>
                  </div>
                  <p className="text-xs text-slate-400">
                    Configured keys: {[settingsDraft.openAiKey, settingsDraft.anthropicKey, settingsDraft.geminiKey, settingsDraft.groqKey].filter(Boolean).length}
                  </p>
                </div>

                <div className="rounded-2xl border border-border/70 bg-background/50 p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-medium text-slate-800">Runtime options</p>
                      <p className="text-xs text-slate-400">Toggle Ollama and tune the base URL.</p>
                    </div>
                    <Dialog>
                      <DialogTrigger asChild>
                        <Button type="button" variant="outline" className="border-border/70 bg-transparent text-slate-700">
                          <SlidersHorizontal className="h-4 w-4" />
                          Edit runtime
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="sm:max-w-lg">
                        <DialogHeader>
                          <DialogTitle>Runtime options</DialogTitle>
                          <DialogDescription>Use Ollama only if you have a running local instance.</DialogDescription>
                        </DialogHeader>

                        <div className="grid gap-4 py-2">
                          <SettingsField
                            label="Ollama base URL"
                            value={settingsDraft.ollamaBaseUrl}
                            onChange={(value) => setSettingsDraft((current) => ({ ...current, ollamaBaseUrl: value }))}
                            placeholder="http://localhost:11434"
                          />

                          <label className="flex items-start gap-3 rounded-2xl border border-border/70 bg-background/50 p-4">
                            <input
                              type="checkbox"
                              checked={settingsDraft.includeOllama}
                              onChange={(event) =>
                                setSettingsDraft((current) => ({ ...current, includeOllama: event.target.checked }))
                              }
                              className="mt-1 h-4 w-4 rounded border-border/70 bg-transparent"
                            />
                            <span className="grid gap-1">
                              <span className="text-sm text-slate-800">Include Ollama</span>
                              <span className="text-xs leading-5 text-slate-400">
                                Enable this if your local Ollama server is running on localhost.
                              </span>
                            </span>
                          </label>
                        </div>

                        <DialogFooter>
                          <DialogClose asChild>
                            <Button type="button">Done</Button>
                          </DialogClose>
                        </DialogFooter>
                      </DialogContent>
                    </Dialog>
                  </div>
                  <p className="flex items-center gap-2 text-xs text-slate-400">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-300" />
                    {settingsDraft.includeOllama ? 'Ollama enabled' : 'Ollama disabled'}
                  </p>
                </div>
              </div>

                <div className="rounded-2xl border border-border/70 bg-background/50 p-4">
                <p className="text-xs uppercase tracking-[0.24em] text-slate-400">Backend endpoint</p>
                <p className="mt-1 text-sm text-slate-800">{BACKEND_API_BASE_URL}</p>
              </div>

                  <div className="flex flex-wrap gap-3">
                    <Button type="submit" disabled={configSaving} className="w-full sm:w-auto px-5">
                  {configSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Server className="h-4 w-4" />}
                  {configSaving ? 'Saving settings' : 'Save settings and refresh'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                      className="border-border/70 bg-transparent text-slate-700 hover:bg-accent hover:text-accent-foreground w-full sm:w-auto"
                  onClick={() => setSettingsDraft(DEFAULT_SETTINGS)}
                >
                  Reset settings
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

            <Toaster position="top-right" />

        <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-slate-900">
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
                          : 'border-border/70 bg-transparent text-slate-700 hover:bg-accent hover:text-accent-foreground'
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
                  No providers are configured yet. Add keys above, save, and refresh.
                </div>
              )}
            </div>

            <Separator className="bg-border/70" />

            <div className="space-y-2">
              <Label htmlFor="model" className="text-slate-700">
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

        {/* <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-slate-900">
              <ArrowRight className="h-5 w-5 text-emerald-300" />
              Prompt composer
            </CardTitle>
            <CardDescription>Send one prompt to the selected provider and model.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="prompt" className="text-slate-700">
                  Prompt
                </Label>
                <Textarea
                  id="prompt"
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  rows={8}
                  className="min-h-[160px] md:min-h-[220px] bg-background/80 text-base leading-7"
                />
              </div>

              <div className="flex flex-wrap gap-3">
                  <Button type="submit" disabled={loading || !selectedModel} className="w-full sm:w-auto px-5">
                  {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                  {loading ? 'Running request' : 'Send prompt'}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                    className="border-border/70 bg-transparent text-slate-700 hover:bg-accent hover:text-accent-foreground w-full sm:w-auto"
                  onClick={() => setPrompt('Summarize the current project in three bullet points.')}
                >
                  Load sample
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                    className="text-slate-700 hover:bg-slate-100 w-full sm:w-auto"
                  onClick={() => setAppliedSettings({ ...settingsDraft })}
                >
                  <RefreshCcw className="h-4 w-4" />
                  Refresh library
                </Button>
              </div>
            </form>
          </CardContent>
        </Card> */}

        {/* <Card className="border-border/60 bg-card/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl text-slate-900">
              <AlertCircle className="h-5 w-5 text-cyan-300" />
              Response
            </CardTitle>
            <CardDescription>Model output and backend errors appear here.</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="min-h-[180px] md:min-h-[240px] rounded-2xl border border-border/70 bg-background/70 p-4 text-sm leading-7 text-slate-800">
              {responseText ? (
                <div className="markdown-body">
                  <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>{responseText}</ReactMarkdown>
                </div>
              ) : (
                'The model response will appear here.'
              )}
            </div>
          </CardContent>
        </Card> */}

        {/* ── Chat UI ── */}
        <ChatPanel
          appliedSettings={appliedSettings}
          selectedModel={selectedModel}
          modelsByProvider={modelsByProvider}
        />
      </main>
    </div>
  );
}

export default App;
