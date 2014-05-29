import os
import shutil
import sys

def pack(name, version):
    curdir = os.path.abspath('.')
    bundle = os.path.join(curdir, 'bundle')
    javadoc = os.path.join(curdir, name, 'target', name + '-' + version + '-sources.jar')
    sources = os.path.join(curdir, name, 'target', name + '-' + version + '-javadoc.jar')
    build = os.path.join(curdir, name, 'target', name + '-' + version + '.jar')
    pom = os.path.join(curdir, name, 'pom.xml')
    
    os.chdir(name)
    if os.path.exists('bundle'): os.system('rm -rf ' + bundle)
    if os.path.exists(javadoc): os.system('rm -f ' + javadoc)
    if os.path.exists(sources): os.system('rm -f ' + sources)
    os.mkdir(bundle)
    
    os.system('mvn javadoc:jar')
    os.system('mvn source:jar')
    
    target_javadoc = os.path.join(bundle, os.path.basename(javadoc))
    target_sources = os.path.join(bundle, os.path.basename(sources))
    target_build = os.path.join(bundle, os.path.basename(build))
    target_pom = os.path.join(bundle, name + '-' + version + '.pom')
    
    shutil.copyfile(javadoc, target_javadoc)
    shutil.copyfile(sources, target_sources)
    shutil.copyfile(build, target_build)
    shutil.copyfile(pom, target_pom)
    
    os.chdir(bundle)
    os.system('gpg -ab ' + target_javadoc)
    os.system('gpg -ab ' + target_sources)
    os.system('gpg -ab ' + target_build)
    os.system('gpg -ab ' + target_pom)
    
    os.system('jar -cvf bundle.jar ' + ' '.join(os.listdir(bundle)))
    
if __name__ == '__main__':
    name, version = sys.argv[1:]
    pack(name, version)