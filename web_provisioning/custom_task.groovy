                script{
                    // Использование ссылочного креда с типом Vault SSH private key with signed public key Credential
                    withCredentials([[$class: 'VaultSignedSSHKeyCredentialBinding',
                        credentialsId: 'ssh_syngxdevops2', // ID ссылочного креда с типом Vault SSH private key with signed public key Credential
                        privateKeyVar: 'SSH_PRIVATE_KEY',           // Переменная, где будет храниться путь к файлу со значением приватного ключа
                        passphraseVar: 'SSH_PRIVATE_KEY_PASS']]) {  // Переменная, где будет хранится значение парольной фразы
                 
                        sshagent([]){
                            sh '''
                            set +x
                            echo -e "#!/bin/sh\necho \\$SSH_PRIVATE_KEY_PASS" > send_ps.sh
                            chmod +x ./send_ps.sh
                            DISPLAY=1 SSH_ASKPASS="./send_ps.sh" ssh-add $SSH_PRIVATE_KEY < /dev/null
                            '''
                            sh """
                            ssh -o StrictHostKeyChecking=no -o CertificateFile=$SIGNED_SSH_PUBLIC_KEY -i $SSH_PRIVATE_KEY $SYNGX_USER@$DEST_HOST 'rm -fr /opt/syngx/html/web_new'
                            ssh -o StrictHostKeyChecking=no -o CertificateFile=$SIGNED_SSH_PUBLIC_KEY -i $SSH_PRIVATE_KEY $SYNGX_USER@$DEST_HOST 'ln -s /opt/provisioning/web_provisioning/build /opt/syngx/html/web_new'
                            """
                        }
                    }
                }
