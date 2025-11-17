#!/usr/bin/env python3
"""
Test schema extraction on one model to understand the structure
"""

import json
import asyncio
from playwright.async_api import async_playwright

async def test_model():
    model_name = "openai/sora-2-pro"
    main_url = f"https://replicate.com/{model_name}"
    
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        print(f"Loading {main_url}...")
        await page.goto(main_url, wait_until="load", timeout=30000)
        await page.wait_for_timeout(10000)  # Wait longer for React to hydrate
        
        # Wait for any script tag to appear
        try:
            await page.wait_for_selector('script', timeout=5000)
        except:
            pass
        
        # Get all possible schema locations
        result = await page.evaluate("""
            () => {
                const result = {
                    has_next_data_script: false,
                    has_window_next_data: false,
                    next_data_structure: null,
                    schema_locations: [],
                    all_script_ids: []
                };
                
                // Check __NEXT_DATA__ script tag
                const nextScript = document.getElementById('__NEXT_DATA__');
                if (nextScript) {
                    result.has_next_data_script = true;
                }
                
                // Check window.__NEXT_DATA__
                if (window.__NEXT_DATA__) {
                    result.has_window_next_data = true;
                }
                
                // Get all script IDs
                document.querySelectorAll('script').forEach((s, i) => {
                    if (s.id) {
                        result.all_script_ids.push(s.id);
                    }
                });
                
                // Check React component props scripts for model data
                const reactScripts = document.querySelectorAll('script[id^="react-component-props"]');
                for (const script of reactScripts) {
                    try {
                        const props = JSON.parse(script.textContent);
                        if (props.model) {
                            result.schema_locations.push(`react-script-${script.id}`);
                            // Get model keys to see structure
                            const modelKeys = Object.keys(props.model);
                            result.model_keys_sample = modelKeys.slice(0, 20);
                            
                            // Check for schema in model
                            if (props.model.openapi_schema) {
                                result.schema_locations.push(`${script.id}.model.openapi_schema`);
                            }
                            if (props.model.latest_version) {
                                result.has_latest_version = true;
                                const lvKeys = Object.keys(props.model.latest_version);
                                result.latest_version_keys = lvKeys.slice(0, 20);
                                if (props.model.latest_version.openapi_schema) {
                                    result.schema_locations.push(`${script.id}.model.latest_version.openapi_schema`);
                                }
                            }
                            if (props.model.version) {
                                result.has_version = true;
                                const vKeys = Object.keys(props.model.version);
                                result.version_keys = vKeys.slice(0, 20);
                                if (props.model.version.openapi_schema) {
                                    result.schema_locations.push(`${script.id}.model.version.openapi_schema`);
                                }
                            }
                        }
                    } catch (e) {}
                }
                
                // Try to get data from either source
                let data = null;
                if (nextScript) {
                    try {
                        data = JSON.parse(nextScript.textContent);
                        result.has_next_data_script = true;
                    } catch (e) {}
                } else if (window.__NEXT_DATA__) {
                    data = window.__NEXT_DATA__;
                    result.has_window_next_data = true;
                }
                
                if (data) {
                    try {
                        const data = JSON.parse(nextScript.textContent);
                        result.next_data_structure = {
                            has_props: !!data.props,
                            has_pageProps: !!data.props?.pageProps,
                            has_model: !!data.props?.pageProps?.model,
                            model_keys: data.props?.pageProps?.model ? Object.keys(data.props.pageProps.model) : [],
                            has_latest_version: !!data.props?.pageProps?.model?.latest_version,
                            latest_version_keys: data.props?.pageProps?.model?.latest_version ? Object.keys(data.props.pageProps.model.latest_version) : [],
                            has_openapi_schema: !!data.props?.pageProps?.model?.openapi_schema,
                            has_versions: !!data.props?.pageProps?.model?.versions,
                            versions_count: data.props?.pageProps?.model?.versions?.length || 0
                        };
                        
                        // Try to find schema
                        const model = data.props?.pageProps?.model;
                        if (model) {
                            if (model.openapi_schema) {
                                result.schema_locations.push('model.openapi_schema');
                            }
                            if (model.latest_version?.openapi_schema) {
                                result.schema_locations.push('model.latest_version.openapi_schema');
                            }
                            if (model.versions) {
                                model.versions.forEach((v, i) => {
                                    if (v.openapi_schema) {
                                        result.schema_locations.push(`model.versions[${i}].openapi_schema`);
                                    }
                                });
                            }
                        }
                    } catch (e) {
                        result.error = String(e);
                    }
                }
                
                return result;
            }
        """)
        
        print("\n=== Analysis ===")
        print(json.dumps(result, indent=2))
        
        # Now try to extract the actual schema from React component props
        if result['schema_locations']:
            print("\n=== Extracting Schema ===")
            schema = await page.evaluate("""
                () => {
                    // Check all React component props scripts
                    const reactScripts = document.querySelectorAll('script[id^="react-component-props"]');
                    for (const script of reactScripts) {
                        try {
                            const props = JSON.parse(script.textContent);
                            if (props.model) {
                                const model = props.model;
                                // Try latest_version first
                                if (model.latest_version?.openapi_schema) {
                                    return {
                                        source: `${script.id}.model.latest_version`,
                                        schema: model.latest_version.openapi_schema
                                    };
                                }
                                // Try direct openapi_schema
                                if (model.openapi_schema) {
                                    return {
                                        source: `${script.id}.model`,
                                        schema: model.openapi_schema
                                    };
                                }
                                // Try version
                                if (model.version?.openapi_schema) {
                                    return {
                                        source: `${script.id}.model.version`,
                                        schema: model.version.openapi_schema
                                    };
                                }
                            }
                        } catch (e) {}
                    }
                    return null;
                }
            """)
            
            if schema:
                print(f"✓ Found schema in: {schema['source']}")
                print(f"Schema keys: {list(schema['schema'].keys())}")
                print(f"\nInput schema exists: {'input' in schema['schema']}")
                print(f"Output schema exists: {'output' in schema['schema']}")
                
                # Save sample
                with open('test_schema_sample.json', 'w') as f:
                    json.dump(schema['schema'], f, indent=2)
                print("\n✓ Saved sample to test_schema_sample.json")
            else:
                print("✗ Could not extract schema")
        
        await browser.close()

if __name__ == "__main__":
    asyncio.run(test_model())

