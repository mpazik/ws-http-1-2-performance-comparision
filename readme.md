# Performance comparison of HTTP1, HTTP2, WebSocket
It's a **naive** end to end test to check performance and latency of different protocols by doing serial request.
Test is a bit unfair as http2 is only protocol that use tsl however in my use case I don't care about security but browsers
requires tsl for http2.

## Result
Nr of serial requests: 100 in chrome browser to localhost server. To enable test check the flag chrome://flags/#allow-insecure-localhost.
- **http1** 332ms; latency: 3.32ms, min: 2ms, max: 47ms
- **http2** - 301ms; latency: 3.01ms, min: 2ms, max: 5ms
- **ws** - 36ms; latency: 0.36ms, min: 0ms, max: 1ms

Server processing times:
(result comes from [performance snapshot](./test.nps) taken by VisualVM)
- **http1** 27.1ms
- **http2** - 84.5ms
- **ws** - 16ms

## Interpretations
- It take a lot of time for browser to do http request comparing to WS.
- After investigation it turned out that from almost 50% request are in stalled state and for 10% are queued 
- WS seems better when low client to server latency is required
- Browser has better performance making http2 requests than http1. Both protocols reuse same connection, no http2 multiplexing is involved, so I guess that's because there is no need of re-sending headers in http2.
- http2 processing takes longer mostly due to ssl but also a bit because header caching and stream prioritization which makes this protocol a bit more complicated than http1
- http1 i slower than ws to process by server mostly because of headers