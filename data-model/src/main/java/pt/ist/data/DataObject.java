package pt.ist.data;

import java.util.HashSet;
import java.util.Set;
import pt.ist.authentication.User;

public class DataObject extends DataObject_Base {

    public void init(DataObjectType type, String label, String externalizedValue, User author) {
        DataObjectVersion firstVersion = new DataObjectVersion();
        firstVersion.init(type, label, externalizedValue, author);
        addVersion(firstVersion);
    }

    public DataObject fork(User author, SynchronizationPolicy sourcePolicy, SynchronizationPolicy targetPolicy) {
        DataObjectVersion lastVersion = getLastVersion();
        DataObject forkedDataObject = new DataObject();
        forkedDataObject.init(lastVersion.getType(), lastVersion.getLabel(), lastVersion.getExternalizedValue(), author);
        SynchronizationConfiguration synchronizationConfiguration = new SynchronizationConfiguration();
        synchronizationConfiguration.init(author, this, forkedDataObject, sourcePolicy, targetPolicy);
        return forkedDataObject;
    }

    public void createNewVersionByMergingWith(User author, DataObjectVersion dataObjectVersion) {
        DataObjectVersion newVersion = new DataObjectVersion();
        newVersion.init(dataObjectVersion.getType(), dataObjectVersion.getLabel(), dataObjectVersion.getExternalizedValue(), author);
        newVersion.setSourceMergeVersion(dataObjectVersion);
        addVersion(newVersion);
    }

    public void setExternalizedValue(String externalizedValue, User author) {
        DataObjectVersion lastVersion = getLastVersion();
        DataObjectVersion newVersion = new DataObjectVersion();
        newVersion.init(lastVersion.getType(), lastVersion.getLabel(), externalizedValue, author);
        addVersion(newVersion);
        DataObjectVersion currentVersion = newVersion;
        while(currentVersion != null) {
            if(currentVersion.getDataObject().hasAnySourceSynchronizationConfiguration()) {
                handleSourceSynchronizationConfigurations(currentVersion.getDataObject().getSourceSynchronizationConfigurationSet());
            }
            if(currentVersion.getDataObject().hasAnyTargetSynchronizationConfiguration()) {
                handleTargetSynchronizationConfigurations(currentVersion.getDataObject().getTargetSynchronizationConfigurationSet());
            }
            currentVersion = currentVersion.getPreviousVersion();
        }
    }

    private void handleSourceSynchronizationConfigurations(Set<SynchronizationConfiguration> sourceConfigurationSet) {
        for(SynchronizationConfiguration sourceConfig : sourceConfigurationSet) {
            if(sourceConfig.getSourcePolicy().equals(SynchronizationPolicy.AUTO)) {
                sourceConfig.getTargetDataObject().createNewVersionByMergingWith(sourceConfig.getAuthor(), getLastVersion());
            }
        }
    }

    private void handleTargetSynchronizationConfigurations(Set<SynchronizationConfiguration> targetConfigurationSet) {
        for(SynchronizationConfiguration targetConfig : targetConfigurationSet) {
            if(targetConfig.getTargetPolicy().equals(SynchronizationPolicy.AUTO)) {
                targetConfig.getSourceDataObject().createNewVersionByMergingWith(targetConfig.getAuthor(), getLastVersion());
            }
        }
    }

    @Override
    public void addVersion(DataObjectVersion newVersion) {
        DataObjectVersion lastVersion = getLastVersion();
        newVersion.setPreviousVersion(lastVersion);
        super.addVersion(newVersion);
        setLastVersion(newVersion);
    }

    public Set<MergeRequest> getMergeRequestSet(User user) {
        Set<MergeRequest> mergeRequestSet = new HashSet<MergeRequest>();
        DataObjectVersion currentVersion = getLastVersion();
        while(currentVersion != null) {
            if(currentVersion.getDataObject().hasAnySourceSynchronizationConfiguration()) {
                for(SynchronizationConfiguration sourceConf : currentVersion.getDataObject().getSourceSynchronizationConfigurationSet()) {
                    if(!sourceConf.getTargetDataObject().getLastVersion().getExternalizedValue().equals(getLastVersion().getExternalizedValue())) {
                        mergeRequestSet.add(new MergeRequest(getLastVersion(), sourceConf.getTargetDataObject()));
                    }
                }
            }
            if(currentVersion.getDataObject().hasAnyTargetSynchronizationConfiguration()) {
                for(SynchronizationConfiguration targetConf : currentVersion.getDataObject().getTargetSynchronizationConfigurationSet()) {
                    if(!targetConf.getTargetDataObject().getLastVersion().getExternalizedValue().equals(getLastVersion().getExternalizedValue())) {
                        mergeRequestSet.add(new MergeRequest(getLastVersion(), targetConf.getTargetDataObject()));
                    }
                }
            }
            currentVersion = currentVersion.getPreviousVersion();
        }

        return mergeRequestSet;
    }
}