import { notFound } from "next/navigation";
import { ViewerClient } from "./viewer-client";
import { getStreamRepository } from "../../../lib/stream-repository";

type ViewerPageProps = {
  params: {
    token: string;
  };
};

export default async function ViewerPage({ params }: ViewerPageProps) {
  const [latest, history] = await Promise.all([
    getStreamRepository().getLatestFrame(params.token),
    getStreamRepository().getHistory(params.token)
  ]);

  if (latest == null || history == null) {
    notFound();
  }

  return (
    <main className="page">
      <div className="container stack">
        <ViewerClient
          token={params.token}
          initialLatest={latest}
          initialHistory={history}
        />
      </div>
    </main>
  );
}
