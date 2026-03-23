import type { Metadata } from "next";

interface Props {
  params: { id: string };
  children: React.ReactNode;
}

async function fetchEvent(id: string) {
  try {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8083";
    const res = await fetch(`${apiUrl}/v1/events/${id}`, { next: { revalidate: 60 } });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const event = await fetchEvent(params.id);
  if (!event) return { title: "Sự kiện | Ticketbox" };

  const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3000";
  const pageUrl = `${siteUrl}/events/${params.id}`;
  const description = event.description
    ? event.description.slice(0, 160)
    : `Sự kiện tại ${event.location}`;

  return {
    title: `${event.title} | Ticketbox`,
    description,
    openGraph: {
      title: event.title,
      description,
      url: pageUrl,
      siteName: "Ticketbox",
      type: "website",
      ...(event.imageUrl && {
        images: [{ url: event.imageUrl, width: 1200, height: 630, alt: event.title }],
      }),
    },
    twitter: {
      card: "summary_large_image",
      title: event.title,
      description,
      ...(event.imageUrl && { images: [event.imageUrl] }),
    },
  };
}

export default function EventDetailLayout({ children }: Props) {
  return <>{children}</>;
}
