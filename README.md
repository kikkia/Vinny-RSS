# Vinny-RSS
Service to handle rss subscriptions for Vinny

## Reddit OAuth

Reddit feeds use application-only OAuth. Configure the service with these
environment variables, which Docker Compose forwards from the host:

- `REDDIT_CLIENTID` — installed-app client ID (required).
- `REDDIT_USERAGENT` — application user agent (required).
- `REDDIT_DEVICEID` — unique, stable 20–30 character ASCII identifier (optional;
  generated once per process if omitted). Set it to retain the same ID across restarts.

No client secret, user login, or redirect URI is needed for public feeds.

## Docker

Replace the placeholders below, then build and start the service:

```sh
export REDDIT_CLIENTID='<installed-app-client-id>'
export REDDIT_USERAGENT='<application-user-agent>'
# Optional: retain the same device ID across restarts.
# export REDDIT_DEVICEID='<unique-20-30-character-id>'

docker compose up --build
```

For background operation, use `docker compose up -d --build`.
Follow logs with `docker compose logs -f rss`, or stop with `docker compose down`.
