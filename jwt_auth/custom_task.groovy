                sshagent(['TYZ_SSH']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER mkdir -p /opt/provisioning/rsa_keys
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER chmod -R 777 /opt/provisioning/rsa_keys
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER chmod -R 777 /opt/provisioning/$PRJ_NAME/tmp
                        scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ./*.pem ${SSH_USER}@${DEST_HOST}:/opt/provisioning/rsa_keys/
                        scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ./$PRJ_NAME/custom_files/* ${SSH_USER}@${DEST_HOST}:/opt/provisioning/$PRJ_NAME/tmp/
                        scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ./secret_id.wrap ${SSH_USER}@${DEST_HOST}:/opt/provisioning/$PRJ_NAME/tmp/
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER sudo /usr/bin/chown -R $LOCAL_USER:$LOCAL_USER /opt/provisioning
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER chmod 755 -R /opt/provisioning/
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER /opt/provisioning/$PRJ_NAME/bin/$PRJ_NAME -c /opt/provisioning/$PRJ_NAME/env/.env -m
                    """
                }
