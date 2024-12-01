# Spring Boot Docker Image Deployment to GCP Artifact Registry

This repository contains the setup for deploying a Spring Boot application Docker image to Google Cloud Platform's Artifact Registry using GitHub Actions and Workload Identity Federation. There are two options for setting up the infrastructure:

Option 1: Using GCP CLI Commands
Option 2: Using Terraform

## Prerequisites

Before starting the setup, ensure you have the following prerequisites in place:

## Required Accounts and Access
1. Google Cloud Platform (GCP) Account
   - Active billing account enabled
   - Owner or Editor role permissions
   - Project created in GCP Console

2. GitHub Account
   - Repository with Spring Boot application code
   - Permissions to add GitHub Actions workflows
   - Access to repository settings

## Local Development Environment

### 1. Install Google Cloud SDK
```bash
# Download and install from:
https://cloud.google.com/sdk/docs/install

# Verify installation
gcloud --version

# Initialize and authenticate
gcloud init
gcloud auth login
```

### 2. Install Docker
```bash
# Download and install from:
https://docs.docker.com/get-docker/

# Verify installation
docker --version
```

### 3. Install Terraform (Required for Option 2 only)
```bash
# Download and install from:
https://developer.hashicorp.com/terraform/downloads

# Verify installation
terraform --version
```

## Enable Required GCP APIs

```bash
# Windows single line command
gcloud services enable iamcredentials.googleapis.com && gcloud services enable iam.googleapis.com && gcloud services enable artifactregistry.googleapis.com

# Expanded version
gcloud services enable iamcredentials.googleapis.com
gcloud services enable iam.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

## Configure GCP Project

1. Set your project ID:
```bash
# List your projects
gcloud projects list

# Set the active project
gcloud config set project YOUR_PROJECT_ID
```

2. Verify project configuration:
```bash
# Check current project
gcloud config get-value project

# Check enabled APIs
gcloud services list --enabled
```

## Required Permissions

Ensure your GCP account has the following roles:
- `roles/iam.workloadIdentityPoolAdmin`
- `roles/iam.serviceAccountAdmin`
- `roles/artifactregistry.admin`

```bash
# Grant necessary roles (if needed)
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="user:YOUR_EMAIL@domain.com" \
    --role="roles/iam.workloadIdentityPoolAdmin"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="user:YOUR_EMAIL@domain.com" \
    --role="roles/iam.serviceAccountAdmin"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="user:YOUR_EMAIL@domain.com" \
    --role="roles/artifactregistry.admin"
```

## Repository Requirements

1. Dockerfile in repository root
2. Valid Spring Boot application
3. `.github/workflows` directory (will be created during setup)

## Environment Variables Needed

Make note of the following values that you'll need during setup:
- `YOUR_PROJECT_ID`: Your GCP project ID
- `YOUR_PROJECT_NUMBER`: Your GCP project number (can be found in project settings)
- `YOUR_GITHUB_USERNAME`: Your GitHub username
- `YOUR_REPO_NAME`: Your GitHub repository name
- `YOUR_REPOSITORY`: Name for your Artifact Registry repository
- `YOUR_IMAGE_NAME`: Name for your Docker image

## Verify Setup

Run these commands to ensure everything is properly installed and configured:

```bash
# Check GCloud SDK
gcloud --version

# Check Docker
docker --version

# Check Terraform (if using Option 2)
terraform --version

# Verify GCP authentication
gcloud auth list

# Check project configuration
gcloud config get-value project
```

## Next Steps

Once all prerequisites are met, you can proceed with either:
- Option 1: Setup using GCP CLI Commands
- Option 2: Setup using Terraform

Choose the option that best fits your infrastructure management preferences.

## Option 1: Setup using GCP CLI Commands

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

## Option 2: Setup using Terraform

### 1. Create Terraform Configuration Files

Create a new directory for your Terraform configuration and create the following files:

`main.tf`:
```hcl
# Configure the Google Cloud provider
provider "google" {
  project = var.project_id
  region  = "asia-south1"
}

# Variables
variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository in format 'owner/repository'"
  type        = string
}

# Workload Identity Pool
resource "google_iam_workload_identity_pool" "github_pool" {
  workload_identity_pool_id = "github-pool-new"
  display_name             = "GitHub Actions Pool"
  description             = "Identity pool for GitHub Actions"
}

# Workload Identity Provider
resource "google_iam_workload_identity_pool_provider" "github_provider" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_pool.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-provider"
  display_name                       = "GitHub provider"
  
  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
  }
  
  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

# Service Account
resource "google_service_account" "github_actions" {
  account_id   = "github-actions"
  display_name = "GitHub Actions Service Account"
  description  = "Service account for GitHub Actions"
}

# Artifact Registry Repository
resource "google_artifact_registry_repository" "docker_repo" {
  location      = "asia-south1"
  repository_id = var.repository_name
  description   = "Docker repository for container images"
  format        = "DOCKER"
}

# IAM binding for Artifact Registry
resource "google_artifact_registry_repository_iam_member" "github_actions_writer" {
  location   = google_artifact_registry_repository.docker_repo.location
  repository = google_artifact_registry_repository.docker_repo.name
  role       = "roles/artifactregistry.writer"
  member     = "serviceAccount:${google_service_account.github_actions.email}"
}

# IAM binding for Workload Identity User
resource "google_service_account_iam_member" "workload_identity_user" {
  service_account_id = google_service_account.github_actions.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/projects/${var.project_id}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_pool.workload_identity_pool_id}/attribute.repository/${var.github_repo}"
}

# Outputs
output "workload_identity_provider" {
  description = "Workload Identity Provider resource name"
  value       = "projects/${var.project_id}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.github_pool.workload_identity_pool_id}/providers/${google_iam_workload_identity_pool_provider.github_provider.workload_identity_pool_provider_id}"
}

output "service_account_email" {
  description = "Service Account email"
  value       = google_service_account.github_actions.email
}

output "artifact_registry_repository" {
  description = "Artifact Registry repository path"
  value       = "${google_artifact_registry_repository.docker_repo.location}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.docker_repo.repository_id}"
}
```

`terraform.tfvars`:
```hcl
project_id     = "YOUR_PROJECT_ID"
github_repo    = "YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"
repository_name = "YOUR_REPOSITORY"
```

### 2. Initialize and Apply Terraform Configuration
```bash
# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Apply configuration
terraform apply
```

### 3. Update GitHub Actions Workflow
Use the outputs from Terraform to update your GitHub Actions workflow file (same as in Option 1).

### Terraform Teardown Process

To destroy all resources created by Terraform:

```bash
# Preview what will be destroyed
terraform plan -destroy

# Destroy resources
terraform destroy
```

## Additional Terraform Troubleshooting

1. **Terraform State Issues**:
   - Ensure you're in the correct directory
   - Check if `terraform.tfstate` exists
   - Verify state is not corrupted

2. **Provider Authentication**:
   - Ensure you're authenticated with GCP
   - Check if required APIs are enabled
   - Verify project permissions

3. **Resource Dependencies**:
   - Let Terraform handle the deletion order
   - If stuck, check for external dependencies
   - Manual cleanup might be needed for locked resources


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
