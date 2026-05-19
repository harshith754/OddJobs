import { notFound } from "next/navigation";
import { ViewerClient } from "./viewer-client";
import { getStreamRepository } from "../../../lib/stream-repository";

type ViewerPageProps = {
  params: Promise<{
    token: string;
  }>;
};

export default async function ViewerPage({ params }: ViewerPageProps) {
  const { token } = await params;
  const [latest, history] = await Promise.all([
    getStreamRepository().getLatestFrame(token),
    getStreamRepository().getHistory(token)
  ]);

  if (latest == null || history == null) {
    notFound();
  }

  return (
    <main className="page">
      <div className="container stack">
        <ViewerClient
          token={token}
          initialLatest={latest}
          initialHistory={history}
        />
      </div>
    </main>
  );
}
