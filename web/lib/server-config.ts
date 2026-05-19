export type ServerPersistenceConfig = {
  appUrl: string;
  supabaseUrl: string | null;
  supabaseSecretKey: string | null;
  storageBucket: string;
  streamToken: string;
};

export function getServerPersistenceConfig(): ServerPersistenceConfig {
  return {
    appUrl: process.env.ODDJOBS_APP_URL ?? "http://localhost:3000",
    supabaseUrl: process.env.SUPABASE_URL ?? null,
    supabaseSecretKey:
      process.env.SUPABASE_SECRET_KEY ??
      process.env.SUPABASE_SERVICE_ROLE_KEY ??
      null,
    storageBucket: process.env.SUPABASE_STORAGE_BUCKET ?? "stream-images",
    streamToken: process.env.ODDJOBS_STREAM_TOKEN ?? "main-frame-stream"
  };
}

export function hasSupabasePersistence(config: ServerPersistenceConfig): boolean {
  return Boolean(config.supabaseUrl && config.supabaseSecretKey);
}
