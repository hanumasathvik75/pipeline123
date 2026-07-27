import sys
import subprocess

def install_and_run():
    try:
        import openai
    except ImportError:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "openai"])
        import openai
        
    from openai import OpenAI

    client = OpenAI(
      base_url = "https://integrate.api.nvidia.com/v1",
      api_key = "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"
    )

    completion = client.chat.completions.create(
      model="meta/llama-3.1-8b-instruct",
      messages=[{"role":"user","content":"Write a short greeting."}],
      temperature=0.2,
      top_p=0.7,
      max_tokens=1024,
      stream=False
    )

    print(completion.choices[0].message.content)

if __name__ == "__main__":
    install_and_run()
