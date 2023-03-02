# EC-NoArtifact

NOTE: the functionality is now available in the product, https://docs.beescloud.com/docs/cloudbees-cd/latest/deploy-automation/third-party-deployment

CloudBees CD DSL: Self-service Catalog item to create an application model that updates environment inventory without an artifact version

For legacy type deployments where artifacts are pushed to application servers, CloudBees CD uses the artifact version value to update environment inventory. However, when deployments are done by another tool, through an API or from a local command line you may still want to have CloudBees CD manage the deployment, updating inventory and performing other deployment tasks, for example. The generated application model can be used as the basis for these types of situations.

The application model created from the self-service catalog item will have as many components as specified and will accept version numbers for each of those components in the deploy process. When run, the model will perform an empty deployment which updates the environment model with the components and version data. If no version is supplied for a particular component, the component deployment will be skipped.

Instructions
- Apply the DSL code in "No-artifact Application SSC.groovy" to create the Self-service Catalog item
- Run the "No Artifact Application" self-service catalog item and provide you inputs

Note:
- The deployment Stage Artifact option must be disabled for this model to work
