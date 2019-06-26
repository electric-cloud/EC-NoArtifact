project 'Default'
catalog 'No Artifact', {
  iconUrl = null
  projectName = 'Default'

  catalogItem 'No Artifact Application', {
    description = '''<xml>
  <title>
    No deploy application model that updates environment inventory
  </title>

  <htmlData>
    <![CDATA[
      This item creates environments and an application model with components and component version input parameters. When run, the environment inventory is updated with the component version. No actual deployment takes place.<p>Smart Deploy and Artifact Staging must be disabled.</p>
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
      "ec_parameterForm": "<editor> <formElement> <label>Application Name</label> <property>app</property> <documentation>Name of the application to be created.</documentation> <type>entry</type> <required>1</required> </formElement> <formElement> <label>Project Name</label> <property>projName</property> <documentation>Name of the application and environment project. e.g. \'Default\'</documentation> <type>project</type> <required>1</required> </formElement> <formElement> <label>Component Version List</label> <property>comps</property> <value>comp1=1.0</value><documentation>Name of the components and versions to be created. On component per line, componentname=version</documentation> <type>textarea</type> <required>1</required> </formElement><formElement> <label>Environment List</label> <property>envs</property> <documentation>Name of environments to be created, comma separated.  E.g., Dev,Int,QA</documentation> <type>entry</type> <required>1</required> </formElement></editor>"
    }],
    "endTarget": {
      "object": "application",
      "formValue": "app"
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


def AgentHost = "localhost"
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
										ectool createArtifact group "\\$[/myComponent/ec_content_details/artifactName]"
									""".stripIndent(),
								shellToUse: ""
							]
							applicationTierName = null
							processStepType = \'command\'
							subprocedure = \'RunCommand\'
							subproject = \'/plugins/EC-Core/project\'
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
				
					processStep comp, {
						processStepType = \'process\'
						subcomponent = comp
						subcomponentApplicationName = applicationName
						subcomponentProcess = \'Install\'
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