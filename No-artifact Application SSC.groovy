/*

CloudBees CD DSL: Self-service Catalog item to create an application model that updates environment inventory without an artifact version

For legacy type deployments where artifacts are pushed to application servers, CloudBees CD uses the artifact version value to update environment inventory. However, when deployments are done by another tool, through an API or from a local command line you may still want to have CloudBees CD manage the deployment, updating inventory and performing other deployment tasks, for example. The generated application model can be used as the basis for these types of situations.

The application model created from the self-service catalog item will have as many components as specified and will accept version numbers for each of those components in the deploy process. When run, the model will perform an empty deployment which updates the environment model with the components and version data. If no version is supplied for a particular component, the component deployment will be skipped.

Instructions
- Apply the DSL code in "No-artifact Application SSC.groovy" to create the Self-service Catalog item
- Run the "No Artifact Application" self-service catalog item and provide you inputs

Note:
- The deployment Stage Artifact option must be disabled for this model to work

*/

project 'Default'
catalog 'No Artifact', {
  iconUrl = null
  projectName = 'Default'

  catalogItem 'No Artifact Application', {
    description = '''<xml>
  <title>
    No artifact application model that updates environment inventory
  </title>

  <htmlData>
    <![CDATA[
      This item creates environments and an application model with components and component version input parameters. When run, the environment inventory is updated with the component version. No actual deployment takes place. You can use this as a starting point for creating models that deploy using other mechanisms. Smart Deploy will examine the existing inventory and skip that component if the target version is already there. If no version is supplied, the component will be skipped<p>Artifact Staging must be disabled.</p>
    ]]>
  </htmlData>
</xml>'''
    buttonLabel = 'Create'
    catalogName = 'No Artifact'
    dslParamForm = '''{
  "sections": {
    "section": [{
      "name": "Application details",
      "instruction": "Provide details required to create the new application.",
      "ec_parameterForm": "<editor> <formElement> <label>Application Name</label> <property>app</property> <documentation>Name of the application to be created.</documentation> <type>entry</type> <required>1</required> </formElement> <formElement> <label>Project Name</label> <property>projName</property> <documentation>Name of the application and environment project. e.g. \'Default\'</documentation> <type>project</type> <required>1</required> </formElement> <formElement> <label>Component Version List</label> <property>comps</property> <value>comp1=1.0\\ncomp2=2.0</value><documentation>Name of the components and versions to be created. On component per line, componentname=version</documentation> <type>textarea</type> <required>1</required> </formElement><formElement> <label>Environment List</label> <property>envs</property> <documentation>Name of environments to be created, comma separated.  E.g., Dev,Int,QA</documentation> <type>entry</type> <required>1</required> </formElement></editor>"
    }],
	"endTarget": {
      "source": "form",
      "object": "application",
      "objectName": "app",
      "objectProjectName": "projName"
    }
  }
}'''
    dslString = '''def proj = args.projName
def app = args.app
// Convert multi line componentName=version to map
def components = [:]
args.comps.split(\'\\n\').each { equal ->
	def pair=equal.split("=")
	components[pair[0]]=pair[1]
}
def environments = args.envs.split(",")


def AgentHost = getResource(resourceName:"local").hostName
def GroupName = "group" // Artifact group name
def Tier = "App Tier"

project proj,{

	environments.each { Env ->
		environment environmentName: Env, {
			def res = "${proj}_${Env}_${Tier}"
			environmentTier Tier, {
				resource resourceName: res, hostName : AgentHost
			}
		}
	} // each environment
	
	application app, {
		applicationTier Tier, {
			components.each { comp, ver ->
				component comp, pluginName: null, {
					pluginKey = \'EC-Artifact\'

					process \'Install\', {
						applicationName = null
						processType = \'DEPLOY\'
						
						processStep \'Create Artifact Placeholder\', {
							actualParameter = [
								commandToRun: """\\
										artifact artifactKey: "\\$[/myComponent/ec_content_details/artifactName]", groupId: \\"${GroupName}\\"
									""".stripIndent(),
								shellToUse: "ectool evalDsl --dslFile"
							]
							applicationTierName = null
							processStepType = \'command\'
							subprocedure = \'RunCommand\'
							subproject = \'/plugins/EC-Core/project\'
						}
					} // process
					
					process \'Uninstall\', {
						applicationName = null
						processType = \'UNDEPLOY\'
						processStep 'Uninstall', {
							actualParameter = [commandToRun: 'echo "Uninstalling $[/myComponent]"']
							applicationTierName = null
							processStepType = 'command'
							subprocedure = 'RunCommand'
							subproject = '/plugins/EC-Core/project'
						}	
					} // process
					
					property \'ec_content_details\', {

						// Custom properties

						property \'artifactName\', value: comp, {
							expandable = \'1\'
						}
						artifactVersionLocationProperty = \'/myJob/retrievedArtifactVersions/$[assignedResourceName]\'
						filterList = \'\'
						overwrite = \'update\'
						pluginProcedure = \'Retrieve\'

						property \'pluginProjectName\', value: \'EC-Artifact\', {
							expandable = \'1\'
						}
						retrieveToDirectory = \'\'

						property \'versionRange\', value: "\\$[${comp}_version]", {
							expandable = \'1\'
						}
					}
						
						
					} // component
				} // each component
			} // tier

			process \'Deploy\', {
				processType = \'OTHER\'

				components.each { comp, ver ->
					formalParameter "${comp}_version", defaultValue: ver
					
					processStep 'No op', {
						actualParameter = [
							'commandToRun': 'echo',
						]
						applicationTierName = 'App Tier'
						processStepType = 'command'
						subprocedure = 'RunCommand'
						subproject = '/plugins/EC-Core/project'
					}					
				
					processStep comp, {
						processStepType = \'process\'
						subcomponent = comp
						subcomponentApplicationName = applicationName
						subcomponentProcess = \'Install\'
						applicationTierName = Tier
					}
					
					processDependency 'No op', targetProcessStepName: comp, {
						branchCondition = "\\$[/javascript myJob[\\"${comp}_version\\"]?true:false]"
						branchConditionName = "Run ${comp}"
						branchConditionType = 'CUSTOM'
						branchType = 'ALWAYS'
					}					
					
				} // each component
			} // process
				
			process \'Undeploy\', {
				processType = \'OTHER\'

				components.each { comp, ver ->
					processStep comp, {
						processStepType = \'process\'
						subcomponent = comp
						subcomponentApplicationName = applicationName
						subcomponentProcess = \'Uninstall\'
						applicationTierName = Tier
					}
				} // each component
			} // process
			
		environments.each { env ->
			tierMap env, {
				environmentName = env
				environmentProjectName = projectName
				
				tierMapping "${Tier}_${env}", {
					applicationTierName = Tier
					environmentTierName = Tier
				}
			}
		} // each environment

	} // application
} // project'''
    endTargetJson = null
    iconUrl = 'icon-process.svg'
    subpluginKey = null
    subprocedure = null
    subproject = null
    useFormalParameter = '0'
  }
}