# GitHub Actions to GCP Artifact Registry Integration Guide

This guide provides detailed instructions for configuring GitHub Actions to securely push Docker images to Google Cloud Platform's Artifact Registry using Workload Identity Federation.


## Table of Contents


1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Google Cloud Configuration Steps](#google-cloud-configuration-steps)
   - [Setting Up Workload Identity Pool](#setting-up-workload-identity-pool)
   - [Creating a Workload Identity Provider](#creating-a-workload-identity-provider)
   - [Creating a Service Account](#creating-a-service-account)
   - [Setting Up Permissions](#setting-up-permissions)
   - [Creating Artifact Registry Repository](#creating-artifact-registry-repository)
4. [GitHub Workflow Configuration](#github-workflow-configuration)
5. [Troubleshooting](#troubleshooting)
6. [Security Best Practices](#security-best-practices)

## Introduction

This integration uses Workload Identity Federation, which is Google Cloud's recommended approach for authenticating workloads outside GCP without using service account keys. This method is more secure because:

- No long-lived credentials are stored in GitHub
- Authentication is temporary and tied to specific workflows
- Access can be limited to specific repositories and actions
- Permissions are granular and follow the principle of least privilege

## Prerequisites

- A Google Cloud Platform (GCP) account with billing enabled
- A GitHub repository with the code for your containerized application
- A Dockerfile in your repository's root directory
- Admin permissions on your GitHub repository
- Editor or Admin permissions on your GCP project

## Google Cloud Configuration Steps

### Setting Up Workload Identity Pool

1. **Open the Google Cloud Console**: Go to [https://console.cloud.google.com/](https://console.cloud.google.com/)

2. **Navigate to Workload Identity Pools**:
   - Go to "IAM & Admin" > "Workload Identity Pools" in the left navigation menu

3. **Create a New Pool**:
   - Click the "CREATE POOL" button
   - Enter a Pool ID (e.g., `github-pool-new`)
   - Add a display name (e.g., "GitHub Actions Pool")
   - Optionally add a description
   - Click "CONTINUE"
   - For the pool state, select "Enabled"
   - Click "CREATE"

### Creating a Workload Identity Provider

1. **Access the Pool**:
   - Click on the newly created pool name (e.g., "github-pool-new")

2. **Add a Provider**:
   - Click "ADD PROVIDER"
   - Provider ID: `github-provider`
   - Display name: "GitHub Actions Provider"
   - Issuer URL: `https://token.actions.githubusercontent.com`
   - Click "CONTINUE"

3. **Configure Attribute Mappings**:
   - Add the following mappings by clicking "ADD MAPPING" for each:
     - `google.subject = assertion.sub`
     - `attribute.actor = assertion.actor`
     - `attribute.repository = assertion.repository`
   - Click "CONTINUE"

4. **Set Attribute Condition**:
   - Add the condition that restricts authentication to your specific repository:
   ```
   assertion.repository == "YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"
   ```
   - Replace `YOUR_GITHUB_USERNAME/YOUR_REPO_NAME` with your actual GitHub repository name (e.g., `pradeepkl/order-microservice`)
   - Click "SAVE"

### Creating a Service Account

1. **Navigate to Service Accounts**:
   - Go to "IAM & Admin" > "Service Accounts" in the left navigation menu

2. **Create a Service Account**:
   - Click "CREATE SERVICE ACCOUNT"
   - Name: `github-actions`
   - Description: "Service account for GitHub Actions"
   - Click "CREATE AND CONTINUE"
   - Skip the "Grant this service account access to project" section for now
   - Click "DONE"

### Setting Up Permissions

1. **Grant Artifact Registry Access**:
   - Go to "IAM & Admin" > "IAM"
   - Click "GRANT ACCESS"
   - In the "New principals" field, enter your service account email:
     ```
     github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com
     ```
   - Select the role "Artifact Registry Writer"
   - Click "SAVE"

2. **Allow Workload Identity Federation to Impersonate the Service Account**:
   - Go to "IAM & Admin" > "Service Accounts"
   - Click on your service account
   - Go to the "PERMISSIONS" tab
   - Click "GRANT ACCESS"
   - In the "New principals" field, enter:
     ```
     principalSet://iam.googleapis.com/projects/YOUR_PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool-new/attribute.repository/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME
     ```
   - Replace `YOUR_PROJECT_NUMBER` with your actual GCP project number
   - Replace `YOUR_GITHUB_USERNAME/YOUR_REPO_NAME` with your GitHub repository name
   - Select the role "Workload Identity User"
   - Click "SAVE"

### Creating Artifact Registry Repository

1. **Navigate to Artifact Registry**:
   - Go to "Artifact Registry" > "Repositories" in the left navigation menu

2. **Create a Repository**:
   - Click "CREATE REPOSITORY"
   - Name: Enter a name for your repository (e.g., `order-microservice-repo`)
   - Format: Select "Docker"
   - Location type: Select "Region"
   - Region: Choose a region (e.g., `asia-south1`)
   - Click "CREATE"

3. **Enable Required APIs**:
   - Go to "APIs & Services" > "Library"
   - Search for and enable these APIs:
     - Artifact Registry API
     - IAM Credentials API
     - Cloud Resource Manager API
     - Security Token Service API

## GitHub Workflow Configuration

Create a new file in your repository at `.github/workflows/build-push-gcp.yml` with the following content:

```yaml
name: Build and Push to GCP
on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]
env:
  PROJECT_ID: YOUR_PROJECT_ID
  GAR_LOCATION: YOUR_REGION
  REPOSITORY: YOUR_ARTIFACT_REGISTRY_REPO_NAME
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

Replace the following placeholders with your actual values:
- `YOUR_PROJECT_ID`: Your GCP project ID
- `YOUR_PROJECT_NUMBER`: Your GCP project number
- `YOUR_REGION`: Your Artifact Registry region (e.g., `asia-south1`)
- `YOUR_ARTIFACT_REGISTRY_REPO_NAME`: The name of your Artifact Registry repository
- `YOUR_IMAGE_NAME`: The name for your Docker image
- `YOUR_GITHUB_USERNAME/YOUR_REPO_NAME`: Your GitHub repository name

## Explanation of GitHub Workflow

Let's break down the GitHub workflow file:

1. **Trigger Configuration**:
   ```yaml
   on:
     push:
       branches: [ "master" ]
     pull_request:
       branches: [ "master" ]
   ```
   This section defines when the workflow will run - on pushes and pull requests to the master branch.

2. **Environment Variables**:
   ```yaml
   env:
     PROJECT_ID: YOUR_PROJECT_ID
     GAR_LOCATION: YOUR_REGION
     REPOSITORY: YOUR_ARTIFACT_REGISTRY_REPO_NAME
     IMAGE_NAME: YOUR_IMAGE_NAME
   ```
   These variables are used throughout the workflow to avoid repetition and make the workflow more maintainable.

3. **Permissions**:
   ```yaml
   permissions:
     contents: read
     id-token: write
   ```
   This is crucial for Workload Identity Federation. The `id-token: write` permission allows the workflow to request an OIDC token from GitHub, which is exchanged for GCP credentials.

4. **Checkout Step**:
   ```yaml
   - name: Checkout repository
     uses: actions/checkout@v4
   ```
   This step checks out your repository code so it's available to the workflow.

5. **Google Authentication**:
   ```yaml
   - name: Google Auth
     id: auth
     uses: google-github-actions/auth@v2
     with:
       workload_identity_provider: '...'
       service_account: '...'
       token_format: 'access_token'
   ```
   This step authenticates with Google Cloud using Workload Identity Federation. It exchanges the GitHub OIDC token for a Google Cloud access token.

6. **Docker Authentication**:
   ```yaml
   - name: Docker Auth
     uses: 'docker/login-action@v3'
     with:
       registry: '${{ env.GAR_LOCATION }}-docker.pkg.dev'
       username: 'oauth2accesstoken'
       password: '${{ steps.auth.outputs.access_token }}'
   ```
   This step uses the access token from the previous step to authenticate with the Artifact Registry Docker repository.

7. **Build and Push Container**:
   ```yaml
   - name: Build and Push Container
     uses: docker/build-push-action@v5
     with:
       context: .
       push: true
       tags: |
         ...
   ```
   This step builds your Docker image from the Dockerfile in your repository and pushes it to Artifact Registry with two tags: the commit SHA and 'latest'.

## Troubleshooting

### Common Issues and Solutions

1. **"Identity Pool does not exist" Error**:
   - Verify the correct project number is used in the workflow file
   - Check if the pool was created in the correct project
   - Ensure the pool name matches exactly (case sensitive)

2. **Authentication Failure**:
   - Wait 5-10 minutes after setting up Workload Identity Federation (permissions can take time to propagate)
   - Verify the attribute condition correctly references your repository
   - Check that the service account has the necessary permissions

3. **Docker Push Failure**:
   - Ensure the Artifact Registry repository exists
   - Verify the region is correct
   - Check that the service account has Artifact Registry Writer permissions

4. **API Not Enabled Error**:
   - Enable all required APIs listed in the prerequisites

## Security Best Practices

1. **Use Restrictive Attribute Conditions**:
   - Limit authentication to specific repositories
   - Consider restricting to specific branches for production deployments

2. **Follow Principle of Least Privilege**:
   - Grant only the necessary permissions to your service account
   - Use different service accounts for different purposes

3. **Use a Dedicated Project for Identity Pools**:
   - Maintain pools in a separate project for better security isolation
   - Apply organizational policies to restrict pool creation

4. **Regularly Audit Permissions**:
   - Periodically review who can authenticate and what they can access
   - Remove permissions that are no longer needed

5. **Enable Audit Logging**:
   - Track who is authenticating and what resources they're accessing
   - Set up alerts for suspicious activities

By following this guide, you have established a secure, modern CI/CD pipeline that follows Google Cloud's recommended best practices for authentication and authorization.
