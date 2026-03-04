                sshagent(['TYZ_SSH']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER chmod -R 777 /opt/provisioning/$PRJ_NAME/env
                        scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ./$PRJ_NAME/custom_files/* ${SSH_USER}@${DEST_HOST}:/opt/provisioning/$PRJ_NAME/env/
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER sudo /usr/bin/chown -R $LOCAL_USER:$LOCAL_USER /opt/provisioning
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER chmod 755 -R /opt/provisioning/
                        ssh -o StrictHostKeyChecking=no -l ${SSH_USER} ${DEST_HOST} sudo -u $LOCAL_USER  /opt/provisioning/dect_config_generator/bin/dect_config_generator -c /opt/provisioning/dect_config_generator/env/.env -m
                    """
                }
