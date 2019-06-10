import java.text.SimpleDateFormat
def dateFormat
def date
def formattedDate
def approverusers
def emailusers
int count;

pipeline {
     agent none
     parameters{
choice(name: 'Customer',choices:'Embroker',description: 'Select Customer')
choice(name: 'LOB', choices:'Embroker', description: 'select project')
string(name: 'Support_Ticket', defaultValue: 'EMBROKER_RATE_AND_QUOTE_API', description: 'Enter Release Version without space')
string(name: 'Release_Number', defaultValue: '', description: 'Enter Ticket Number')
}  
    stages {
            stage('Packaging') {
            agent { node {  label 'ConfigTier'  } }
            steps {
               script {
			   try{
                   count =0;
                   wrap([$class: 'BuildUser']) {
                   currentBuild.description = "${Release_Number}-${Support_Ticket} triggered by ${BUILD_USER}"
                   }
                echo "RELEASE INFO \n ================== \n Release Customer: ${Customer}\n Customer LOB:  ${LOB} \n Release_Support Ticket: ${Support_Ticket} \n RELEASE NUMBER: ${Release_Number}"
				echo "Packaging Phase: "
                sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/packagingV4.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				echo "Nexus Uploading: "
				sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/jsoninfoV2.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
                approverusers = readFile('approverslist').trim()
				env.RELEASE_PKGAPPROVAL = input message: 'Package verification', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote Package to proceed Alpha Release')]
				if (env.RELEASE_PKGAPPROVAL == "YES") {
					echo "PackageUpload"
					sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/PackageUpload.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				}
				//else{
				//}
			}
			catch(Exception e){
                echo "${e}"
				retry(100){
				sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
                emailusers = readFile('emailuserslist').trim()
				if(count == 0){
                mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc: "jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Packaging Failed  for ticket  ${Release_Number} \n Fix the issue and click Rerun \n Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )                     			   
			} 
			else{
			mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Packaging Again Failed in  for ticket  ${Release_Number} \n Fix the issue and click Rerun \nRetry count:${count}\n Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
			}
			sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
            approverusers = readFile('approverslist').trim()
			env.RELEASE_SCOPE = input message: 'Task execution  Failed in Packaging  Once issue Fixed Leave Comment and rerun', ok: 'RERUN!', submitter: approverusers,
                            parameters: [string(name: 'Aprroval_string', defaultvalue: '', description: 'Approval comment:')]  
			count =count+1;
			echo "RePackaging for ${count} time:"
                sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/packagingV4.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/jsoninfoV2.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				echo "Nexus Uploading: "
				env.RELEASE_PKGAPPROVAL = input message: 'Package verification', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote Package to proceed Alpha Release')]
                if (env.RELEASE_PKGAPPROVAL == "YES") { 			
					sh '/usr/bin/solartis/CD_Automation/SDPFileCreationV2/PackageUpload.sh ${Customer} ${LOB} ${Support_Ticket} ${Release_Number}'
				}
				//else{
				//}
			}
			}
            }
        }
			post
              {
                 always{
                     script
                     {
							sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
							emailusers = readFile('emailuserslist').trim()
                             mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Pacakging Activity Completed for ticket  ${Release_Number} \n To enter Alpha Environment Waiting for approvals.Following assignees involved to approve:\nGlobal Delivery lead\n Embroker_pm\nKindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL} " )
                 }
             }
	     }
	}        
		
        stage('Alpha') {
            agent { node {  label 'Stable_ConfigTier'  } }
            steps {
                script {
                    try{
                    count =0;
                     wrap([$class: 'BuildUser']) {
                    currentBuild.description = "${Release_Number}-${Support_Ticket} triggered by ${BUILD_USER}"
							sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
							emailusers = readFile('emailuserslist').trim()
				   			 mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Started for ${Release_Number} in Alpha \n Release Started by: ${BUILD_USER} \n click below link to check log:\n For blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL} "  )
                   }
                sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
                }
                }
                catch(Exception e){
                    		echo "${e}"
				retry(100){
				sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
                emailusers = readFile('emailuserslist').trim()
				if(count == 0){
                mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Failed in Alpha for ticket  ${Release_Number} \n Fix the issue and click Rerun \n Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )                     			   
			} 
			else{
			mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Again Failed in ALPHA for ticket  ${Release_Number} \n Fix the issue and click Rerun \nRetry count:${count}\n Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
			}
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
				approverusers = readFile('approverslist').trim()
				echo approverusers
			env.RELEASE_SCOPE = input message: 'Task execution  Failed in ALPHA  Once issue Fixed Leave Comment and rerun', ok: 'RERUN!', submitter: approverusers,
                            parameters: [string(name: 'Aprroval_string', defaultvalue: '', description: 'Approval comment:')]  
			count =count+1;
		
                sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
				
                }
                
                }       
				}
				
		}
            }
			post
              {
                 always{
                     script
                     {
						sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
						emailusers = readFile('emailuserslist').trim()
                             mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Release Success  in alpha for ticket  ${Release_Number} \n To enter Beta Environment Waiting for approvals.Following assignees involved to approve:\nadmin\nbauser\nPM\nKindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL} " )
						sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_BA > approverslist"
						approverusers = readFile('approverslist').trim()
							env.RELEASE_SCOPE2 = input message: 'BA Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote BETA Release')]
							if (env.RELEASE_SCOPE2 == "NO") {
							sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
							approverusers = readFile('approverslist').trim()
								env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
								parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Need to Rollback')]
				
								if (env.RELEASE_TESTAPPROVAL == "YES") {
									echo "Rollback"
									//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
								}
				
							}
                 }
             }
	     }

        }
        
        stage('Beta') {
		agent { node {  label 'QA_Hybrid'  } }
            steps {
                script {
				   try{
				   count =0;
                   wrap([$class: 'BuildUser']) {
                   currentBuild.description = "${Release_Number}-${Support_Ticket} triggered by ${BUILD_USER}"
                   }
                sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
                }
                }
				catch(Exception e)
				{
 				echo "${e}"
				retry(100){
				sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
                emailusers = readFile('emailuserslist').trim()
				if(count == 0){
                mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Failed in BETA for ticket  ${Release_Number} \n Fix the issue and click Rerun. Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
                     			   
			} 
			else{
			mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Again Failed in BETA for ticket  ${Release_Number} \n Fix the issue and click Rerun Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
			}
			
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
				approverusers = readFile('approverslist').trim()
			env.RELEASE_SCOPE = input message: 'Task execution  Failed in BETA  Once issue Fixed Leave Comment and rerun', ok: 'RERUN!', submitter: approverusers,
                            parameters: [string(name: 'Aprroval_string', defaultvalue: '', description: 'Approval comment:')]  
			count =count+1;
				sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
                }
				}
				}
            }
			}
			
			 post
              {
                 always{
                     script
                     {
							sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
							emailusers = readFile('emailuserslist').trim()
							echo emailusers
                             mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Release Success  in BETA for ticket  ${Release_Number} \n To enter RC Environment Wait for approvals.Following assignees involved to approve:\nadmin\nqauser\nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
						sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_QA > approverslist"
						approverusers = readFile('approverslist').trim()
						env.RELEASE_SCOPE2 = input message: 'QA Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote RC Release')]
						if (env.RELEASE_SCOPE2 == "NO") {
						sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
						approverusers = readFile('approverslist').trim()
							env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
							parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
					    if (env.RELEASE_TESTAPPROVAL == "YES") {
							echo "Rollback"
							//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
						}
						}
						sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
						approverusers = readFile('approverslist').trim()
						env.RELEASE_SCOPE3 = input message: 'PM Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote RC Release')]
						if (env.RELEASE_SCOPE3 == "NO") {
						sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
						approverusers = readFile('approverslist').trim()
							env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
							parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
						if (env.RELEASE_TESTAPPROVAL == "YES") {
							echo "Rollback"
							//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
						}
						}
					 }
                 }
             }
             }
	 	  stage('RC') {
		agent { node {  label 'RackSpace-UAT'  } }
            steps {
                script {
				   try{
				   count =0;
                   wrap([$class: 'BuildUser']) {
                   currentBuild.description = "${Release_Number}-${Support_Ticket} triggered by ${BUILD_USER}"
                   }
  
                sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
                }
                }
				catch(Exception e)
				{
 				echo "${e}"
				retry(100){
				sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
                emailusers = readFile('emailuserslist').trim()
				echo emailusers
				if(count == 0){
                mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Failed in RC for ticket  ${Release_Number} \n Fix the issue and click Rerun \nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )                     			   
			} 
			else{
			mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Again in BETA for ticket  ${Release_Number} \n Fix the issue and click Rerun \nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
			}
			sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
				approverusers = readFile('approverslist').trim()
			env.RELEASE_SCOPE = input message: 'Task execution Failed in BETA  Once issue Fixed Leave Comment and rerun', ok: 'RERUN!', submitter: approverusers,
                            parameters: [string(name: 'Aprroval_string', defaultvalue: '', description: 'Approval comment:')]  
			count =count+1;
				sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}' 
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
			    env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
				
                }
                
				}
				}
             }
			}

			 post
              {
                 always{
                     script
                     {
					 sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
					 emailusers = readFile('emailuserslist').trim()
					 echo emailusers
                             mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Release Success  in RC for ticket  ${Release_Number} \n To enter RC Environment Wait for approvals.Following assignees involved to approve:\nadmin\nqauser\nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
											dateFormat = new SimpleDateFormat("HH:mm:ss")
				   sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_QA > approverslist"
				approverusers = readFile('approverslist').trim()
					env.RELEASE_SCOPE3 = input message: 'QA Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote GA Release')]
					if (env.RELEASE_SCOPE3 == "NO") {
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
					approverusers = readFile('approverslist').trim()
						env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
						parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
						if (env.RELEASE_TESTAPPROVAL == "YES") {
							echo "Rollback"
							//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
					}
					}	
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
					approverusers = readFile('approverslist').trim()
                    env.RELEASE_SCOPE2 = input message: 'PM Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote GA Release')]
					if (env.RELEASE_SCOPE2 == "NO") {
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
					approverusers = readFile('approverslist').trim()
						env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
						parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
						if (env.RELEASE_TESTAPPROVAL == "YES") {
							echo "Rollback"
							//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
					}
					}
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh BUSUSER > approverslist"
					approverusers = readFile('approverslist').trim()
					env.RELEASE_SCOPE3 = input message: 'BUS Approval Required', ok: 'Release!', submitter: approverusers,
                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'NO\nYES', description: 'Promote GA Release')]
					if (env.RELEASE_SCOPE3 == "NO") {
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
					approverusers = readFile('approverslist').trim()
						env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
						parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
						if (env.RELEASE_TESTAPPROVAL == "YES") {
							echo "Rollback"
							//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
					}
					}
					}
                     
                 }
             }
    }
        stage('GA') {
		agent { node {  label 'amtools_prod_hybrid'  } }
            steps {
                script {
				try{
				 count =0;
                   wrap([$class: 'BuildUser']) {
                   currentBuild.description = "${Release_Number}-${Support_Ticket} triggered by ${BUILD_USER}"
					}
				   dateFormat = new SimpleDateFormat("HH:mm:ss")
				   date = new Date()
				   sh "date +'%T' > result";
                   formattedDate=readFile('result').trim()
			
					if (formattedDate >= '04:30:00 AM' || formattedDate <= '09:30:00 PM' ){
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh INFRA > approverslist"
					approverusers = readFile('approverslist').trim()
					env.RELEASE_SCOPE = input message: 'Production Hours Approval for release', ok: 'Done!', submitter: approverusers,
                            parameters: [choice(name: 'Aprroval_choice', choices: 'NO\nYES', description: 'Approve GA Release')]
					}	
				sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]
					sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
					approverusers = readFile('approverslist').trim()
                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}
                }
     }
              catch(Exception e)	{
			  echo "${e}"
			  retry(100){
			  sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
                emailusers = readFile('emailuserslist').trim()
			  count =count+1;
			  if(count == 0){
               mail(from: "ReleaseUpdates@solartis.com", 
                             to:emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Failed in GA for ticket  ${Release_Number} \n Fix the issue and click Rerun \nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
                     			   
			} 
			else{
			mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",	
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Deployment Failed in GA for ticket  ${Release_Number} \n Fix the issue and click Rerun \nPM Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL}" )
			}
			sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ${LOB}_DEV > approverslist"
				approverusers = readFile('approverslist').trim()
			env.RELEASE_SCOPE = input message: 'Task execution  Failed in BETA  Once issue Fixed Leave Comment and rerun', ok: 'RERUN!', submitter: approverusers,
            parameters: [string(name: 'Aprroval_string', defaultvalue: '', description: 'Approval comment:')]
			    sh 'mkdir -p /opt/CD/${Support_Ticket}-${Release_Number} '
				sh 'cd /opt/CD/ && curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" "http://192.168.85.154:8081/nexus/service/local/repositories/SolartisDeploymentPackage/content/${Customer}/${LOB}/${Support_Ticket}/${Release_Number}/${Support_Ticket}-${Release_Number}.sdp" -O --output /opt/CD/'
				sh 'unzip -o /opt/CD/${Support_Ticket}-${Release_Number}.sdp -d /opt/CD/${Support_Ticket}-${Release_Number}'
				sh '/usr/bin/solartis/CD_Automation/PackDownload/InitialStatusEntryV4.sh ${Support_Ticket}-${Release_Number}'
				echo "SQL Execution and Backup..."
				sh '/usr/bin/solartis/CD_Automation/SQL/sqlbackup_V2.sh ${Support_Ticket}-${Release_Number} '
			    sh '/usr/bin/solartis/CD_Automation/SQL/sqlexecution_V2.sh ${Support_Ticket}-${Release_Number} '
				echo "Forms Backup and Deployment"
		     	sh '/usr/bin/solartis/CD_Automation/Forms/formsdeployV4.sh ${Support_Ticket}-${Release_Number}' 
				echo "KB Backup and Deployment"
				sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/kbdeployV5.sh ${Support_Ticket}-${Release_Number}'
				//sh '/usr/bin/solartis/CD_Automation/KnowledgeBase/NodeRestartCondition.sh ${Support_Ticket}-${Release_Number}'
				sh "/usr/bin/solartis/CD_Automation/Users/approveuser.sh ReleaseEngineering > approverslist"
				approverusers = readFile('approverslist').trim()
				env.RELEASE_TESTAPPROVAL = input message: 'Release Team Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Release Verification Test Completed')]

                if (env.RELEASE_TESTAPPROVAL == "NO") {
					env.RELEASE_TESTAPPROVAL = input message: 'Rollback Approval Required', ok: 'Done!', submitter: approverusers,
                parameters: [choice(name: 'TestVerificationApproval', choices: 'NO\nYES', description: 'Approve to Rollback')]
				if (env.RELEASE_TESTAPPROVAL == "YES") {
					echo "Rollback"
					//sh '/usr/bin/Solartis/CD_Automation/Rollback/rollbackv2.sh ${Support_Ticket}-${Release_Number}'
				}

                }
			  }
			  }			 
              }
            }
		 post
              {
                 always{
                     script
                     {
					sh "/usr/bin/solartis/CD_Automation/Users/emailuser.sh ${LOB} > emailuserslist"
					emailusers = readFile('emailuserslist').trim()
                             mail(from: "ReleaseUpdates@solartis.com", 
                             to: emailusers, 
							 cc:"jothilakshmi_m@solartis.com,ranjithkumar_s@solartis.com,karthickraja_m@solartis.com,gopinath_t@solartis.com,shyamsundar_r@solartis.com,pavithradevi_p@solartis.com,rajarathinasabapathi_s@solartis.com",
                             subject: "Release-${customer}-${Release_Number}",
                             body: "Release Success  in Alpha,Beta,RC and GA for ticket  ${Release_Number} \n Release completed\n Kindly check log in bellow link:\nFor blue ocean view: ${RUN_DISPLAY_URL}\nFor classic view: ${BUILD_URL} " )
                 }
             }
	      }
		  }
    }
  }
