export default function HomePage() {
  return (
    <main className="page">
      <div className="container stack">
        <section className="hero">
          <h1>OddJobs</h1>
          <p className="muted">
            Personal utilities, starting with Frame Stream. Use the Android app
            to capture frames and share a private viewer link.
          </p>
        </section>

        <section className="grid grid-2">
          <div className="panel stack">
            <h2>Frame Stream</h2>
            <p className="muted">
              High-quality still-image streaming for reading screens and text
              remotely.
            </p>
          </div>
          <div className="panel stack">
            <h2>Current MVP</h2>
            <p className="muted">
              One long-lived personal stream, many sessions, latest image view,
              and recent history.
            </p>
          </div>
        </section>
      </div>
    </main>
  );
}

