def remote = [:]
remote.name = "odoo-server"
remote.host = "191.168.0.141"
remote.port = 1919
remote.allowAnyHosts = true
def now = new Date()
time = now.format("dd-MM-yy-HHmm")


node {  
        cleanWs()   
			stage("Application Backup") {
			  withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  sshCommand remote: remote, command: "cd /opt/Application/Rollback/;rm -rf Inventory/*;rm -rf Database/*", failOnError:'false'
				  sshCommand remote: remote, command: "cp -R /opt/Application/InventoryManagement/* /opt/Application/Rollback/Inventory", failOnError:'true'
				  sshCommand remote: remote, command: "mkdir -p /opt/Application/Backup/backup_${time}/Inventory", failOnError:'true'
				  sshCommand remote: remote, command: "mkdir -p /opt/Application/Backup/backup_${time}/Database", failOnError:'true'
				  sshCommand remote: remote, command: "mv /opt/Application/InventoryManagement/* /opt/Application/Backup/backup_${time}/Inventory", failOnError:'true'
				  //sshCommand remote: remote, command: "cd /opt/Application/Backup/;tar -czvf backup_${time}.tar.gz --absolute-names backup_${time}", failOnError:'true'
				  //sshCommand remote: remote, command: "cd /opt/Application/Backup/;rm -rf backup_${time}", failOnError:'true'
				  
	
			  }
        
      }
	  	  stage("Database"){
			withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  withCredentials([usernamePassword(credentialsId: 'psql', passwordVariable: 'pwd', usernameVariable: 'puser')]) {
				      sshCommand remote: remote, command: "PGPASSWORD=zwIDBB6rH6X4k pg_dump -h 127.0.0.1 -p 5434 -U odoo -d Beta > /opt/Application/Rollback/Database/database.bak", failOnError:'true'
				      sshCommand remote: remote, command: "PGPASSWORD=zwIDBB6rH6X4k pg_dump -h 127.0.0.1 -p 5434 -U odoo -d Beta > /opt/Application/Backup/backup_${time}/Database/database.bak", failOnError:'true'
					  sshCommand remote: remote, command: "cd /opt/Application/Backup/;tar -czvf backup_${time}.tar.gz --absolute-names backup_${time}", failOnError:'true'
				      sshCommand remote: remote, command: "cd /opt/Application/Backup/;rm -rf backup_${time}", failOnError:'true'

				}
				  
	  }
}
        stage ("Git Check Out") {
            git branch: 'master', credentialsId: 'odoo', url: 'https://github.com/ArielPremium/InventoryManagement'
        }
        
        
		stage("Deploy") {
			  handler = ""
			  withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  sshRemove remote: remote, path: "/home/devuser/deploy/Deploy"
				  sshPut remote: remote, from: './', into: '/home/devuser/deploy/'
				  sshCommand remote: remote, command: 'mv /home/devuser/deploy/Deploy/* /opt/Application/InventoryManagement/', failOnError:'true'
				  sshCommand remote: remote, command: 'chmod 755 /opt/Application/InventoryManagement/odoo-bin', failOnError:'true'
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

	stage("Status") {
			  withCredentials([sshUserPrivateKey(credentialsId: 'key', keyFileVariable: 'identity', usernameVariable: 'user')]) {
				  remote.user = user
				  remote.identityFile = identity
				  sshCommand remote: remote, command: 'supervisorctl status', failOnError:'true'
				  
				  
			  }
        
      }

}




