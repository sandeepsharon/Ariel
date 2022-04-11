
def pod = []



def remote = [:]
remote.name = "odoo-server"
remote.host = "191.168.0.141"
remote.port = 1919
remote.allowAnyHosts = true



node {  
        cleanWs()   
			stage("Rollback") {
			  withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  sshCommand remote: remote, command: "ls -1 /opt/Application/Backup/ | tr '\n' ' ' > /home/devuser/t.txt", failOnError:'true'
				  sshCommand remote: remote, command: 'sed -i \'s/ *$//\' /home/devuser/t.txt', failOnError:'true'
				  sshCommand remote: remote, command: 'sed -i \'s/\\s\\+/,/g\' /home/devuser/t.txt', failOnError:'true'
				  sshCommand remote: remote, command: "sed -i 's/^/rollback,/' /home/devuser/t.txt", failOnError:'true'
				  sshCommand remote: remote, command: 'sed -i \'s/.tar.gz//g\' /home/devuser/t.txt', failOnError:'true'

				  pod = sshCommand remote: remote, command: 'cat /home/devuser/t.txt', failOnError:'true'
				  pod = pod.split(',') as List
				  properties([parameters([choice(choices: pod, description: 'Choose Backup to Rollback', name: 'backup')])])
				  if(params.backup == "rollback") 
				  {
				    sshagent(['key']){
				        
                    sh "ssh -t devuser@191.168.0.141 -p 1919 'rm -rf /opt/Application/InventoryManagement/*'"

                    sh "ssh -t devuser@191.168.0.141 -p 1919 'cp -R /opt/Application/Rollback/Inventory/* /opt/Application/InventoryManagement/'"

                    
                }
				withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  withCredentials([usernamePassword(credentialsId: 'psql', passwordVariable: 'pwd', usernameVariable: 'puser')]) {
				      sshCommand remote: remote, command: "PGPASSWORD=zwIDBB6rH6X4k psql -h 127.0.0.1 -p 5434 -U odoo -d Beta -f /opt/Application/Rollback/Database/database.bak", failOnError:'true'
		

				}
				  
			}
				      
				  }
				  else
				  {
				  echo "Rolling back changes from the backup ${params.backup}"
				  sshagent(['key']){
                    sh "ssh -t devuser@191.168.0.141 -p 1919 'cd /opt/Application/Backup/;tar -xzvf ${params.backup}.tar.gz'"
                    sh "ssh -t devuser@191.168.0.141 -p 1919 'rm -rf /opt/Application/InventoryManagement/*'"

                    sh "ssh -t devuser@191.168.0.141 -p 1919 'cd /opt/Application/Backup/;cp -R ${params.backup}/Inventory/* /opt/Application/InventoryManagement/'"
					withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  withCredentials([usernamePassword(credentialsId: 'psql', passwordVariable: 'pwd', usernameVariable: 'puser')]) {
				      sshCommand remote: remote, command: "PGPASSWORD=zwIDBB6rH6X4k psql -h 127.0.0.1 -p 5434 -U odoo -d Beta -f /opt/Application/Backup/${params.backup}/Database/database.bak", failOnError:'true'
		

				}
				  
				}
                    sh "ssh -t devuser@191.168.0.141 -p 1919 'cd /opt/Application/Backup/;rm -rf */'"
                    
                    
                }
				  }
	
				  
					
				  
			  }
        
      }
	  
	  stage("Deploy") {
			  handler = ""
			  withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  String demo = sshCommand remote: remote, command: 'supervisorctl restart all', failOnError:'true'
				  println(demo)
				  
				  try{
				  writeFile file: 'error.txt', text: demo
					sh 'cat error.txt'
            sh "grep 'ERROR' error.txt > errortrap"
            handler = readFile('errortrap').trim()

        }catch(Exception ex){
            println("Odoo")
        }
        echo "${handler}"
        if (handler != ""){
            println("Exception")
            error "Program failed, please read logs..."
			
			}

			  }
        
      }



	  	  

}



