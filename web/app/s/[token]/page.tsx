import { notFound } from "next/navigation";
import { ViewerClient } from "./viewer-client";
import { ensureSeedData, getHistory, getLatestFrame } from "../../../lib/stream-store";

type ViewerPageProps = {
  params: {
    token: string;
  };
};

export default function ViewerPage({ params }: ViewerPageProps) {
  ensureSeedData();
  const latest = getLatestFrame(params.token);
  const history = getHistory(params.token);

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
