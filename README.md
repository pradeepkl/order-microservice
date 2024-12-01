# Spring Boot Docker Image Deployment to GCP Artifact Registry

This repository contains the setup for deploying a Spring Boot application Docker image to Google Cloud Platform's Artifact Registry using GitHub Actions and Workload Identity Federation.

## Prerequisites

- Google Cloud Platform account with billing enabled
- GitHub repository with Spring Boot application
- gcloud CLI installed and initialized
- Docker installed locally (for testing)
- Required GCP APIs enabled:
  ```bash
  # Windows single line
  gcloud services enable iamcredentials.googleapis.com && gcloud services enable iam.googleapis.com && gcloud services enable artifactregistry.googleapis.com
  ```
  ```bash
  # Expanded version
  gcloud services enable iamcredentials.googleapis.com
  gcloud services enable iam.googleapis.com
  gcloud services enable artifactregistry.googleapis.com
  ```

## Initial Setup

### 1. Set Project
```bash
# Verify current project
gcloud config get-value project

# Set project if needed
gcloud config set project YOUR_PROJECT_ID
```

### 2. Create Workload Identity Pool
```bash
# Windows single line
gcloud iam workload-identity-pools create github-pool-new --project=YOUR_PROJECT_ID --location=global --display-name="GitHub Actions Pool"
```
```bash
# Expanded version
gcloud iam workload-identity-pools create "github-pool-new" \
  --project="YOUR_PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions Pool"
```

### 3. Verify Pool Creation
```bash
# Windows single line
gcloud iam workload-identity-pools describe github-pool-new --project=YOUR_PROJECT_ID --location=global
```
```bash
# Expanded version
gcloud iam workload-identity-pools describe "github-pool-new" \
  --project="YOUR_PROJECT_ID" \
  --location="global"
```

### 4. Create Workload Identity Provider
```bash
# Windows single line
gcloud iam workload-identity-pools providers create-oidc github-provider --project=YOUR_PROJECT_ID --location=global --workload-identity-pool=github-pool-new --display-name="GitHub provider" --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" --issuer-uri="https://token.actions.githubusercontent.com" --attribute-condition="assertion.repository=='YOUR_GITHUB_USERNAME/YOUR_REPO_NAME'"
```
```bash
# Expanded version
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --project="YOUR_PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="github-pool-new" \
  --display-name="GitHub provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-condition="assertion.repository=='YOUR_GITHUB_USERNAME/YOUR_REPO_NAME'"
```

### 5. Create Service Account
```bash
# Windows single line
gcloud iam service-accounts create github-actions --project=YOUR_PROJECT_ID --display-name="GitHub Actions Service Account"
```
```bash
# Expanded version
gcloud iam service-accounts create github-actions \
  --project="YOUR_PROJECT_ID" \
  --display-name="GitHub Actions Service Account"
```

### 6. Create Artifact Registry Repository
```bash
# Windows single line
gcloud artifacts repositories create YOUR_REPOSITORY --project=YOUR_PROJECT_ID --repository-format=docker --location=asia-south1 --description="Docker repository for Spring Boot applications"
```
```bash
# Expanded version
gcloud artifacts repositories create YOUR_REPOSITORY \
  --project="YOUR_PROJECT_ID" \
  --repository-format=docker \
  --location=asia-south1 \
  --description="Docker repository for Spring Boot applications"
```

### 7. Configure IAM Permissions
```bash
# Windows single line
gcloud artifacts repositories add-iam-policy-binding YOUR_REPOSITORY --project=YOUR_PROJECT_ID --location=asia-south1 --member="serviceAccount:github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com" --role="roles/artifactregistry.writer"
```
```bash
# Get your project number (needed for next step)
gcloud projects describe YOUR_PROJECT_ID --format="value(projectNumber)"
```
```bash
# Windows single line - Use project number from previous step
gcloud iam service-accounts add-iam-policy-binding github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com --project=YOUR_PROJECT_ID --role="roles/iam.workloadIdentityUser" --member="principalSet://iam.googleapis.com/projects/YOUR_PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool-new/attribute.repository/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"
```

## GitHub Actions Workflow Setup

Create `.github/workflows/build-push.yml` with the following content:

```yaml
name: Build and Push to GCP

on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]

env:
  PROJECT_ID: YOUR_PROJECT_ID
  GAR_LOCATION: asia-south1
  REPOSITORY: YOUR_REPOSITORY
  IMAGE_NAME: YOUR_IMAGE_NAME
  
permissions:
  contents: read
  id-token: write

jobs:
  build-and-push:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout repository
      uses: actions/checkout@v4

    - name: Google Auth
      id: auth
      uses: google-github-actions/auth@v2
      with:
        workload_identity_provider: 'projects/YOUR_PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool-new/providers/github-provider'
        service_account: 'github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com'
        token_format: 'access_token'

    - name: Docker Auth
      uses: 'docker/login-action@v3'
      with:
        registry: '${{ env.GAR_LOCATION }}-docker.pkg.dev'
        username: 'oauth2accesstoken'
        password: '${{ steps.auth.outputs.access_token }}'

    - name: Build and Push Container
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          ${{ env.GAR_LOCATION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/${{ env.REPOSITORY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          ${{ env.GAR_LOCATION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/${{ env.REPOSITORY }}/${{ env.IMAGE_NAME }}:latest
```

## Important Notes

1. Replace placeholders:
   - `YOUR_PROJECT_ID`: Your GCP project ID
   - `YOUR_PROJECT_NUMBER`: Your GCP project number
   - `YOUR_REPOSITORY`: Your Artifact Registry repository name
   - `YOUR_GITHUB_USERNAME`: Your GitHub username
   - `YOUR_REPO_NAME`: Your GitHub repository name
   - `YOUR_IMAGE_NAME`: Desired name for your Docker image

2. Critical Points:
   - Use project number (not project ID) in workload identity provider path
   - Ensure all GCP APIs are enabled before starting
   - Verify resource creation after each step
   - Maintain exact case sensitivity in repository names

## Verification

1. Check Workload Identity Pool:
```bash
gcloud iam workload-identity-pools describe github-pool-new --project=YOUR_PROJECT_ID --location=global
```

2. Verify Service Account:
```bash
gcloud iam service-accounts describe github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com
```

3. Check Artifact Registry Repository:
```bash
gcloud artifacts repositories list --project=YOUR_PROJECT_ID --location=asia-south1
```

## Teardown Process

Follow these steps to clean up all created resources. Execute them in order to properly remove all dependencies.

### 1. Delete Artifact Registry Repository
```bash
# Windows single line
gcloud artifacts repositories delete YOUR_REPOSITORY --project=YOUR_PROJECT_ID --location=asia-south1 --quiet
```
```bash
# Expanded version
gcloud artifacts repositories delete YOUR_REPOSITORY \
  --project=YOUR_PROJECT_ID \
  --location=asia-south1 \
  --quiet
```

### 2. Remove Service Account IAM Bindings
```bash
# Windows single line
gcloud iam service-accounts remove-iam-policy-binding github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com --project=YOUR_PROJECT_ID --role="roles/iam.workloadIdentityUser" --member="principalSet://iam.googleapis.com/projects/YOUR_PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool-new/attribute.repository/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"
```

### 3. Delete Service Account
```bash
# Windows single line
gcloud iam service-accounts delete github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com --project=YOUR_PROJECT_ID --quiet
```
```bash
# Expanded version
gcloud iam service-accounts delete github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com \
  --project=YOUR_PROJECT_ID \
  --quiet
```

### 4. Delete Workload Identity Pool Provider
```bash
# Windows single line
gcloud iam workload-identity-pools providers delete github-provider --project=YOUR_PROJECT_ID --location=global --workload-identity-pool=github-pool-new --quiet
```
```bash
# Expanded version
gcloud iam workload-identity-pools providers delete github-provider \
  --project=YOUR_PROJECT_ID \
  --location=global \
  --workload-identity-pool=github-pool-new \
  --quiet
```

### 5. Delete Workload Identity Pool
```bash
# Windows single line
gcloud iam workload-identity-pools delete github-pool-new --project=YOUR_PROJECT_ID --location=global --quiet
```
```bash
# Expanded version
gcloud iam workload-identity-pools delete github-pool-new \
  --project=YOUR_PROJECT_ID \
  --location=global \
  --quiet
```

### 6. Verify Resource Deletion
```bash
# Check if Artifact Registry repository is deleted
gcloud artifacts repositories list --project=YOUR_PROJECT_ID --location=asia-south1

# Check if Service Account is deleted
gcloud iam service-accounts list --project=YOUR_PROJECT_ID

# Check if Workload Identity Pool is deleted
gcloud iam workload-identity-pools list --project=YOUR_PROJECT_ID --location=global
```

## Troubleshooting

1. **Pool Already Exists Error**:
   - Use `gcloud iam workload-identity-pools list` to check existing pools
   - Create pool with a different name if needed

2. **Invalid Argument for Identity Pool**:
   - Verify project number is used (not project ID)
   - Check repository name case sensitivity
   - Ensure all slashes and quotes are correct

3. **Docker Authentication Error**:
   - Verify `token_format: 'access_token'` is set in workflow
   - Check service account permissions
   - Ensure Artifact Registry API is enabled

4. **Permission Denied**:
   - Verify APIs are enabled
   - Check service account roles
   - Ensure correct project is set

5. **Resource Busy During Deletion**:
   - Wait a few minutes and try again
   - Check for dependencies in the GCP Console
   - Ensure no active workloads are using the resources

6. **Dependencies Still Exist During Deletion**:
   - Follow the deletion order as specified
   - Check for any custom IAM bindings
   - Look for any custom configurations in GCP Console
