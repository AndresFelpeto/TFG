# Base image
FROM python:3.9-slim

# Install dependencies (e.g., nginx for reverse proxy)
RUN apt-get update && apt-get install -y \
    nginx \
    certbot \
    python3-certbot-nginx

# Set up the app (copy your app, install dependencies, etc)
WORKDIR /app
COPY . /app
RUN pip install -r requirements.txt

# Expose the ports (80 for HTTP, 443 for HTTPS)
EXPOSE 80 443

# Command to run your app, e.g. Flask
CMD ["python", "server_multiuser.py"]

