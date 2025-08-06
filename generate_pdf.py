import asyncio
import os
from playwright.async_api import async_playwright

async def generate_pdf_from_html(html_file, pdf_file):
    """
    Renders an HTML file with a headless browser and saves it as a PDF.
    This works for dynamic content rendered by JavaScript.

    Args:
        html_file (str): The path to the input HTML file.
        pdf_file (str): The path for the output PDF file.
    """
    if not os.path.exists(html_file):
        print(f"Error: The HTML file '{html_file}' was not found.")
        return

    # Use an absolute path for the HTML file so the browser can find it
    html_path = os.path.abspath(html_file)
    print(f"Opening HTML file in browser: {html_path}")

    async with async_playwright() as p:
        # Launch a headless Chromium browser
        browser = await p.chromium.launch()
        page = await browser.new_page()

        # Navigate to the local HTML file
        await page.goto(f"file://{html_path}")

        # Wait for the main content to be rendered by JavaScript
        # We wait for the table with statistics to appear.
        try:
            await page.wait_for_selector('table.stats', timeout=10000)
            print("Page content rendered. Generating PDF...")
        except Exception:
            print("Warning: Timed out waiting for page content. Generating PDF anyway.")

        # Generate the PDF from the fully-rendered page
        await page.pdf(path=pdf_file, format='A4', print_background=True)

        await browser.close()

    print(f"Successfully generated PDF report at '{pdf_file}'.")

if __name__ == "__main__":
    # Define the input HTML and desired output PDF filenames
    input_html_file = 'report.html'
    output_pdf_file = 'locust_report.pdf'

    # Run the asynchronous function
    asyncio.run(generate_pdf_from_html(input_html_file, output_pdf_file))
