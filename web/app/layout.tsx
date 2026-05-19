import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "OddJobs Viewer",
  description: "Private frame viewer for OddJobs Frame Stream"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

