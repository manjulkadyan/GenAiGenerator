#!/usr/bin/env python3
"""
Parse schema from HTML tables on the schema page
Since the schema is displayed in tables, we can parse them
"""

import json
import asyncio
from playwright.async_api import async_playwright
from typing import Dict, Any, Optional

async def parse_schema_from_tables(model_name: str, page) -> Optional[Dict[str, Any]]:
    """Parse schema from HTML tables on the schema page"""
    schema_url = f"https://replicate.com/{model_name}/api/schema"
    
    try:
        await page.goto(schema_url, wait_until="networkidle", timeout=30000)
        await page.wait_for_timeout(5000)
        
        # Parse the schema from the rendered HTML tables
        schema = await page.evaluate("""
            () => {
                const result = { input: { type: "object", properties: {} }, output: {} };
                
                // Find Input schema section
                const inputSection = Array.from(document.querySelectorAll('h2, h3, h4')).find(
                    el => el.textContent.toLowerCase().includes('input schema')
                );
                
                if (inputSection) {
                    // Find the table after the heading
                    let current = inputSection.nextElementSibling;
                    while (current && current.tagName !== 'TABLE') {
                        current = current.nextElementSibling;
                    }
                    
                    if (current && current.tagName === 'TABLE') {
                        const rows = current.querySelectorAll('tbody tr');
                        rows.forEach(row => {
                            const cells = row.querySelectorAll('td');
                            if (cells.length >= 2) {
                                const name = cells[0].textContent.trim();
                                const type = cells[1].textContent.trim();
                                const description = cells.length > 2 ? cells[2].textContent.trim() : '';
                                
                                result.input.properties[name] = {
                                    type: type.toLowerCase(),
                                    description: description
                                };
                            }
                        });
                    }
                }
                
                // Find Output schema section
                const outputSection = Array.from(document.querySelectorAll('h2, h3, h4')).find(
                    el => el.textContent.toLowerCase().includes('output schema')
                );
                
                if (outputSection) {
                    let current = outputSection.nextElementSibling;
                    while (current && current.tagName !== 'TABLE') {
                        current = current.nextElementSibling;
                    }
                    
                    if (current && current.tagName === 'TABLE') {
                        const cells = current.querySelectorAll('td');
                        if (cells.length > 0) {
                            result.output = {
                                type: cells[0].textContent.trim().toLowerCase(),
                                description: cells.length > 1 ? cells[1].textContent.trim() : ''
                            };
                        }
                    }
                }
                
                // If we found any properties, return the schema
                if (Object.keys(result.input.properties).length > 0 || result.output.type) {
                    return result;
                }
                
                return null;
            }
        """)
        
        return schema
    except Exception as e:
        print(f"  Error: {e}")
        return None

async def main():
    """Test with one model"""
    model_name = "openai/sora-2-pro"
    print(f"Testing {model_name}...")
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        schema = await parse_schema_from_tables(model_name, page)
        if schema:
            print("✓ Found schema from tables!")
            print(json.dumps(schema, indent=2))
            with open('test_table_schema.json', 'w') as f:
                json.dump(schema, f, indent=2)
        else:
            print("✗ No schema found in tables")
        
        await browser.close()

if __name__ == "__main__":
    asyncio.run(main())

