from fastapi import FastAPI


def create_app(database_path=None, token_secret=None):
    # No API behavior yet; contract tests should fail on missing routes.
    return FastAPI()
