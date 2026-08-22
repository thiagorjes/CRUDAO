import styles from './page.module.css';

type BackendHealth = {
  status: string;
  service: string;
  timestamp: string;
};

export async function getBackendHealth(): Promise<BackendHealth | { error: string }> {
  const backendUrl = process.env.BACKEND_URL ?? 'http://localhost:8080';
  try {
    const res = await fetch(`${backendUrl}/api/health`, { cache: 'no-store' });
    if (!res.ok) {
      return { error: `Backend respondeu com status ${res.status}` };
    }
    return (await res.json()) as BackendHealth;
  } catch {
    return { error: 'Backend indisponível' };
  }
}

export default async function Home() {
  const health = await getBackendHealth();

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <h1>Kanban Configurável — CRUDAO</h1>
        <p>Setup inicial do projeto (TASK-00.2) — chamada de exemplo ao backend:</p>
        {'error' in health ? (
          <p style={{ color: '#dc3545' }}>❌ {health.error}</p>
        ) : (
          <pre style={{ background: '#f8f9fa', padding: '12px', borderRadius: '6px' }}>
            {JSON.stringify(health, null, 2)}
          </pre>
        )}
      </main>
    </div>
  );
}
