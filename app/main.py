from fastapi import FastAPI

app = FastAPI(title="MangaRec API")


@app.get("/health")
def health_check():
    return {"status": "ok"}
