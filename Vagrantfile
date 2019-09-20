# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|

  config.vm.box = "centos/7"

  config.ssh.insert_key = false

  config.vbguest.auto_update = true
 
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  config.vm.synced_folder "build", "/vagrant_build"

  config.vm.provider "virtualbox" do |vb|
     vb.name = "ldapClient-centos"
     vb.gui = false
     vb.memory = "1024"
     vb.cpus = 2
  end

  config.vm.provision "ansible" do |ansible|
    ansible.verbose = "-vvv"
    ansible.playbook = "ansible/ldapClient-playbook.yml"
  end
end
