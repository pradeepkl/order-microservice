# Spring Boot Docker Image Deployment to GCP Artifact Registry

This repository contains configuration for automating the build and deployment of a Spring Boot application Docker image to Google Cloud Platform's Artifact Registry using GitHub Actions. The setup can be done using either CLI commands or Terraform.

## Prerequisites

- Google Cloud Platform account
- GitHub repository with Spring Boot application
- Docker installed locally (for testing)
- GCP Project with billing enabled
- gcloud CLI installed (for Option 1)
- Terraform installed (for Option 2)

## Option 1: Setup using GCP CLI Commands

### 1. Create Workload Identity Pool
```bash
# Creates a new identity pool for GitHub Actions authentication
gcloud iam workload-identity-pools create "github-pool" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --display-name="GitHub Actions Pool"
```

### 2. Create Workload Identity Provider
```bash
# Sets up OIDC provider for secure authentication between GitHub and GCP
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --project="${PROJECT_ID}" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --display-name="GitHub provider" \
  --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
  --issuer-uri="https://token.actions.githubusercontent.com"
```

### 3. Create Service Account
```bash
# Creates a dedicated service account for GitHub Actions
gcloud iam service-accounts create github-actions \
  --project="${PROJECT_ID}"
```

### 4. Configure IAM Permissions
```bash
# Grants artifact registry write permissions to the service account
gcloud artifacts repositories add-iam-policy-binding ${REPOSITORY} \
  --project="${PROJECT_ID}" \
  --location=asia-south1 \
  --member="serviceAccount:github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

# Allows GitHub Actions to impersonate the service account
gcloud iam service-accounts add-iam-policy-binding "github-actions@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="${PROJECT_ID}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_ID}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_REPOSITORY}"
```

## Option 2: Setup using Terraform

### 1. Create Terraform Configuration Files
Create the following files in your repository:

`main.tf`:
```hcl
# Contains the main Terraform configuration for GCP resources
# See terraform script from previous message
```

`terraform.tfvars`:
```hcl
# Configure your specific values
project_id  = "your-project-id"
github_repo = "your-github-org/your-repo-name"
```

### 2. Initialize and Apply Terraform Configuration
```bash
# Initialize Terraform working directory
terraform init

# Preview the changes
terraform plan

# Apply the configuration
terraform apply
```

### 3. Save Terraform Outputs
```bash
# Note down the outputs for:
# - workload_identity_provider
# - service_account_email
# - artifact_registry_repository
```

## GitHub Actions Workflow Setup

### 1. Create GitHub Actions Workflow File
Create `.github/workflows/build-push.yml`:

```yaml
# Workflow configuration that builds and pushes Docker image to GCP
name: Build and Push to GCP

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

env:
  PROJECT_ID: your-project-id
  GAR_LOCATION: asia-south1
  REPOSITORY: your-repository
  IMAGE_NAME: spring-boot-app
  
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
        workload_identity_provider: projects/${{ env.PROJECT_ID }}/locations/global/workloadIdentityPools/github-pool/providers/github-provider
        service_account: github-actions@${{ env.PROJECT_ID }}.iam.gserviceaccount.com

    - name: Docker Auth
      id: docker-auth
      uses: docker/login-action@v3
      with:
        registry: ${{ env.GAR_LOCATION }}-docker.pkg.dev
        username: oauth2accesstoken
        password: ${{ steps.auth.outputs.access_token }}

    - name: Build and Push Container
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          ${{ env.GAR_LOCATION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/${{ env.REPOSITORY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          ${{ env.GAR_LOCATION }}-docker.pkg.dev/${{ env.PROJECT_ID }}/${{ env.REPOSITORY }}/${{ env.IMAGE_NAME }}:latest
```

### 2. Update Environment Variables
Update the following variables in the workflow file:
- `PROJECT_ID`: Your GCP project ID
- `REPOSITORY`: Your Artifact Registry repository name
- `IMAGE_NAME`: Desired name for your Docker image

## Verification

1. Push changes to your GitHub repository
2. Check GitHub Actions tab to monitor the workflow
3. Verify image in GCP Artifact Registry:
```bash
# List images in your repository
gcloud artifacts docker images list \
    asia-south1-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/${IMAGE_NAME}
```

## Important Notes

- All resources are created in the Mumbai region (asia-south1)
- The workflow uses OpenID Connect (OIDC) for secure authentication
- Images are tagged with both git SHA and 'latest' tags
- Service account has minimal required permissions for security
- Workload Identity Federation eliminates the need for static credentials

## Troubleshooting

1. **Authentication Issues**:
   - Verify Workload Identity Pool configuration
   - Check service account permissions
   - Ensure GitHub repository name matches the configuration

2. **Build Failures**:
   - Check Dockerfile syntax
   - Verify Spring Boot application builds locally
   - Review GitHub Actions logs for detailed errors

3. **Push Failures**:
   - Confirm Artifact Registry repository exists
   - Verify service account has proper IAM roles
   - Check network connectivity in GitHub Actions logs

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request
