<template>
  <base-dialog :title="title" :width="width" :visible="dialogVisible"
    :createLabel="createLabel" :cancelLabel="cancelLabel" :error="error"
    @createClicked="onCreateClicked" @cancelClicked="onCancelClicked"
    hideButtons="true">
    <v-stepper v-model="step">
      <v-stepper-header>
        <v-stepper-step step="1" :complete="step > 1">Device Type</v-stepper-step>
        <v-divider></v-divider>
        <v-stepper-step step="2" :complete="step > 2">Asset</v-stepper-step>
        <v-divider></v-divider>
        <v-stepper-step step="3">Metadata<small>Optional</small></v-stepper-step>
      </v-stepper-header>
      <v-stepper-content step="1">
        <v-card flat>
          <v-card-text>
            <v-container fluid>
              <v-layout row wrap>
                <v-flex xs12>
                  <v-text-field required class="mt-1" label="Type name"
                    v-model="typeName" prepend-icon="info"></v-text-field>
                </v-flex>
                <v-flex xs12>
                  <v-select required :items="containerPolicies" v-model="typeContainerPolicy"
                    label="Container policy" prepend-icon="developer_board"></v-select>
                </v-flex>
                <v-flex xs12>
                  <v-select required :items="assetModules" v-model="typeAssetModule"
                    item-text="name" item-value="id" label="Asset module"
                    prepend-icon="local_offer"></v-select>
                </v-flex>
              </v-layout>
            </v-container>
          </v-card-text>
        </v-card>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn flat @click.native="onCancelClicked">{{ cancelLabel }}</v-btn>
          <v-btn color="primary" :disabled="!firstPageComplete" flat
            @click="step = 2">Choose Asset
            <v-icon light>keyboard_arrow_right</v-icon>
          </v-btn>
        </v-card-actions>
      </v-stepper-content>
      <v-stepper-content step="2">
        <asset-chooser :assetModuleId="typeAssetModule" :assetId="typeAssetId"
          @assetUpdated="onAssetUpdated"></asset-chooser>
        <v-card-actions>
          <v-btn color="primary" flat @click.native="step = 1">
            <v-icon light>keyboard_arrow_left</v-icon>
            Back
          </v-btn>
          <v-spacer></v-spacer>
          <v-btn flat @click.native="onCancelClicked">{{ cancelLabel }}</v-btn>
          <v-btn color="primary" flat :disabled="!secondPageComplete"
            @click="onCreateClicked">{{ createLabel }}</v-btn>
          <v-btn color="primary" flat :disabled="!secondPageComplete"
            @click="step = 3">Add Metadata
            <v-icon light>keyboard_arrow_right</v-icon>
          </v-btn>
        </v-card-actions>
      </v-stepper-content>
      <v-stepper-content step="3">
        <metadata-panel class="mb-3" :metadata="metadata"
          @itemDeleted="onMetadataDeleted" @itemAdded="onMetadataAdded"/>
          <v-card-actions>
            <v-btn color="primary" flat @click.native="step = 2">
              <v-icon light>keyboard_arrow_left</v-icon>
              Back
            </v-btn>
            <v-spacer></v-spacer>
            <v-btn flat @click.native="onCancelClicked">{{ cancelLabel }}</v-btn>
            <v-btn color="primary" flat :disabled="!secondPageComplete"
              @click.native="onCreateClicked">{{ createLabel }}</v-btn>
          </v-card-actions>
      </v-stepper-content>
    </v-stepper>
  </base-dialog>
</template>

<script>
import Utils from '../common/Utils'
import BaseDialog from '../common/BaseDialog'
import MetadataPanel from '../common/MetadataPanel'
import AssetChooser from '../common/AssetChooser'
import {_getAssetModules} from '../../http/sitewhere-api-wrapper'

export default {

  data: () => ({
    step: null,
    dialogVisible: false,
    typeName: null,
    typeContainerPolicy: null,
    typeAssetModule: null,
    typeAssetId: null,
    metadata: [],
    assetModules: [],
    containerPolicies: [
      {
        'text': 'Standalone Device',
        'value': 'Standalone'
      }, {
        'text': 'Composite Device',
        'value': 'Composite'
      }
    ],
    error: null
  }),

  components: {
    BaseDialog,
    MetadataPanel,
    AssetChooser
  },

  props: ['title', 'width', 'createLabel', 'cancelLabel'],

  computed: {
    // Indicates if first page fields are filled in.
    firstPageComplete: function () {
      return (!this.isBlank(this.$data.typeName) &&
        this.$data.typeContainerPolicy &&
        this.$data.typeAssetModule)
    },

    // Indicates if second page fields are filled in.
    secondPageComplete: function () {
      return this.firstPageComplete && (this.$data.typeAssetId != null)
    }
  },

  methods: {
    // Generate payload from UI.
    generatePayload: function () {
      var payload = {}
      payload.name = this.$data.typeName
      payload.containerPolicy = this.$data.typeContainerPolicy

      var assetReference = {}
      assetReference.module = this.$data.typeAssetModule
      assetReference.id = this.$data.typeAssetId
      payload.assetReference = assetReference

      payload.metadata = Utils.arrayToMetadata(this.$data.metadata)
      return payload
    },

    // Reset dialog contents.
    reset: function () {
      this.$data.typeName = null
      this.$data.typeContainerPolicy = null
      this.$data.typeAssetModule = null
      this.$data.typeAssetId = null
      this.$data.metadata = []
      this.$data.step = 1

      var component = this
      _getAssetModules(this.$store, 'Device')
        .then(function (response) {
          component.assetModules = response.data
          this.onAssetModulesLoaded()
        }).catch(function (e) {
        })
    },

    // Load dialog from a given payload.
    load: function (payload) {
      this.reset()

      if (payload) {
        this.$data.typeName = payload.name
        this.$data.typeContainerPolicy = payload.containerPolicy
        this.$data.typeAssetModule = payload.assetReference.module
        this.$data.typeAssetId = payload.assetReference.id
        this.$data.metadata = Utils.metadataToArray(payload.metadata)
      }
    },

    // Called to open the dialog.
    openDialog: function () {
      this.$data.dialogVisible = true
    },

    // Called to open the dialog.
    closeDialog: function () {
      this.$data.dialogVisible = false
    },

    // Called to show an error message.
    showError: function (error) {
      this.$data.error = error
    },

    // Called after create button is clicked.
    onCreateClicked: function (e) {
      var payload = this.generatePayload()
      this.$emit('payload', payload)
    },

    // Called after cancel button is clicked.
    onCancelClicked: function (e) {
      this.$data.dialogVisible = false
    },

    // Called when an asset is chosen or removed.
    onAssetUpdated: function (asset) {
      if (asset) {
        this.$data.typeAssetId = asset.id
      } else {
        this.$data.typeAssetId = null
      }
    },

    // Called when a metadata entry has been deleted.
    onMetadataDeleted: function (name) {
      var metadata = this.$data.metadata
      for (var i = 0; i < metadata.length; i++) {
        if (metadata[i].name === name) {
          metadata.splice(i, 1)
        }
      }
    },

    // Called when a metadata entry has been added.
    onMetadataAdded: function (entry) {
      var metadata = this.$data.metadata
      metadata.push(entry)
    },

    // Tests whether a string is blank.
    isBlank: function (str) {
      return (!str || /^\s*$/.test(str))
    }
  }
}
</script>

<style scoped>
</style>
