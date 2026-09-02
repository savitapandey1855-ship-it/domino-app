#!/usr/bin/env python3
"""
Send a Firebase Cloud Messaging (FCM) push notification.
Uses the FCM HTTP v1 API with OAuth 2.0 service account authentication.

Usage:
  python send_notification.py --title "Hello" --body "World" --topic all
  python send_notification.py --title "Hello" --body "World" --token <device_token>
  python send_notification.py --title "Hello" --body "World" --topic all --url https://example.com

Requirements:
  pip install google-auth requests
"""
import argparse, json, sys, requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request

SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"]

def get_access_token(path):
    creds = service_account.Credentials.from_service_account_file(path, scopes=SCOPES)
    creds.refresh(Request())
    return creds.token

def get_project_id(path):
    with open(path) as f:
        return json.load(f)["project_id"]

def send(service_account_path, title, body, topic=None, token=None, url=None):
    project_id = get_project_id(service_account_path)
    access_token = get_access_token(service_account_path)
    endpoint = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    message = {"notification": {"title": title, "body": body}}
    if url:
        message["data"] = {"url": url}
    if topic:
        message["topic"] = topic
    elif token:
        message["token"] = token
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
    response = requests.post(endpoint, headers=headers, json={"message": message})
    if response.status_code == 200:
        print(f"Sent! Response: {response.json()}")
    else:
        print(f"Failed (HTTP {response.status_code}): {response.text}")
        sys.exit(1)

if __name__ == "__main__":
    p = argparse.ArgumentParser(description="Send FCM push notification for Domino")
    p.add_argument("--title", required=True)
    p.add_argument("--body", required=True)
    p.add_argument("--topic", default="all")
    p.add_argument("--token", default=None)
    p.add_argument("--url", default=None)
    p.add_argument("--key-file", default="firebase-service-account.json")
    a = p.parse_args()
    send(a.key_file, a.title, a.body, a.topic, a.token, a.url)
