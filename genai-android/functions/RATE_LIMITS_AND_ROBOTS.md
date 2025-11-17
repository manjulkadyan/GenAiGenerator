# Rate Limits and Robots.txt Information

## Replicate API Rate Limits

Based on Replicate's official API documentation:

### Official API Limits
- **Create Prediction Requests**: 600 requests per minute
- **All Other Endpoints**: 3,000 requests per minute
- **Rate Limit Response**: Returns `429` status code with message:
  ```json
  {"detail":"Request was throttled. Expected available in 1 second."}
  ```

### Search API (Unofficial)
The search API endpoint we're using (`replicate-search-prototype-production.replicate.workers.dev`) is not the official API, so it may have different limits. However, we're being conservative and respecting the same limits.

## Current Scraper Rate Limiting

### Implementation
- **Delay between models**: 200ms
- **Delay between requests** (schema + page for same model): 100ms
- **Total requests per model**: 2 (schema + page)
- **Effective rate**: ~5 requests/second = 300 requests/minute

### Why This is Safe
- Well under the 3,000/min limit for "other endpoints"
- Conservative approach to avoid hitting limits
- Respectful of Replicate's infrastructure

### Example Calculation
For 50 models:
- Total requests: 50 models × 2 requests = 100 requests
- Time: 50 models × 200ms = 10 seconds (plus request time)
- Rate: ~100 requests in ~20 seconds = 300 requests/minute ✅

## Rate Limit Handling

The scraper now includes:

1. **Exponential Backoff**: On 429 errors, waits with exponential backoff (up to 10s)
2. **Retry-After Header**: Respects `Retry-After` header if provided
3. **Automatic Retries**: Up to 3 retries on rate limit errors
4. **Error Logging**: Warns when rate limited but continues

## Robots.txt

### Checking robots.txt

Run:
```bash
npm run check:robots
```

This will:
- Fetch and display `https://replicate.com/robots.txt`
- Check if search API has robots.txt (usually doesn't)
- Display rate limit information

### Important Notes

1. **robots.txt is advisory**: It's not enforceable - it's a guideline for respectful crawlers
2. **We're being respectful**: Our scraper:
   - Uses proper User-Agent headers
   - Implements rate limiting
   - Doesn't overwhelm the server
   - Only fetches public data

3. **Search API**: The search API endpoint is used by Replicate's own website, so it's designed to handle requests, but we should still be respectful.

## Recommendations

1. **Current settings are good**: 200ms delay is conservative and safe
2. **Monitor for 429 errors**: If you see many 429s, increase the delay
3. **Respect robots.txt**: Check it periodically for changes
4. **Use official API when possible**: For production, consider using the official Replicate API with authentication

## Running the Check

```bash
cd genai-android/functions
npm run check:robots
```

This will display:
- robots.txt contents
- Rate limit information
- Current scraper configuration

