#!/usr/bin/env python3
"""
Debug script to see what's actually on the schema page
"""

import json
import asyncio
from playwright.async_api import async_playwright

async def debug_schema_page(model_name: str):
    """Debug what's on the schema page"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)  # Visible to see what's happening
        page = await browser.new_page()
        
        print(f"\n=== Debugging {model_name} ===")
        print(f"URL: {schema_url}")
        
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)
        
        # Get page info
        info = await page.evaluate("""
            () => {
                return {
                    title: document.title,
                    has_next_data: !!document.getElementById('__NEXT_DATA__'),
                    scripts_count: document.querySelectorAll('script').length,
                    pre_tags: document.querySelectorAll('pre').length,
                    code_tags: document.querySelectorAll('code').length,
                    all_text: document.body.innerText.substring(0, 1000),
                    url: window.location.href
                };
            }
        """)
        
        print(f"\nPage Info:")
        print(json.dumps(info, indent=2))
        
        # Check for __NEXT_DATA__
        next_data = await page.evaluate("""
            () => {
                const script = document.getElementById('__NEXT_DATA__');
                if (script) {
                    try {
                        const data = JSON.parse(script.textContent);
                        return {
                            has_data: true,
                            props_keys: data.props ? Object.keys(data.props) : [],
                            pageProps_keys: data.props?.pageProps ? Object.keys(data.props.pageProps) : [],
                            has_model: !!data.props?.pageProps?.model,
                            model_keys: data.props?.pageProps?.model ? Object.keys(data.props.pageProps.model) : []
                        };
                    } catch (e) {
                        return { error: str(e) };
                    }
                }
                return { has_data: false };
            }
        """)
        
        print(f"\n__NEXT_DATA__ Info:")
        print(json.dumps(next_data, indent=2))
        
        # Look for any JSON on the page
        all_json = await page.evaluate("""
            () => {
                const results = [];
                // Check all script tags
                document.querySelectorAll('script').forEach((script, i) => {
                    if (script.textContent && script.textContent.length > 50) {
                        try {
                            const parsed = JSON.parse(script.textContent);
                            results.push({
                                source: `script_${i}`,
                                type: 'script',
                                has_input: 'input' in parsed,
                                has_output: 'output' in parsed,
                                keys: Object.keys(parsed).slice(0, 10)
                            });
                        } catch (e) {}
                    }
                });
                // Check pre/code tags
                document.querySelectorAll('pre, code').forEach((tag, i) => {
                    const text = tag.textContent.trim();
                    if (text.startsWith('{') && text.length > 50) {
                        try {
                            const parsed = JSON.parse(text);
                            results.push({
                                source: `${tag.tagName}_${i}`,
                                type: tag.tagName,
                                has_input: 'input' in parsed,
                                has_output: 'output' in parsed,
                                keys: Object.keys(parsed).slice(0, 10),
                                preview: text.substring(0, 200)
                            });
                        } catch (e) {}
                    }
                });
                return results;
            }
        """)
        
        print(f"\nFound JSON structures:")
        print(json.dumps(all_json, indent=2))
        
        # Wait a bit so user can see
        await asyncio.sleep(3)
        await browser.close()

if __name__ == "__main__":
    # Test with one model
    asyncio.run(debug_schema_page("openai/sora-2-pro"))

