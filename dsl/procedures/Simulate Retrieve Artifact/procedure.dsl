import java.io.File

def procName = 'Simulate Retrieve Artifact'
procedure procName, {

	step 'Update Environment Inventory',
    	  command: new File(pluginDir, "dsl/procedures/$procName/steps/Update Environment Inventory.sh").text,
    	  shell: 'sh'

}
  
