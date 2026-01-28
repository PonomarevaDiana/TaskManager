<script>
export default {
  name: 'PushPanel',
  props: {
    message: {
      type: String,
      required: true
    },
    buttonText: {
      type: String,
      default: 'OK'
    },
    buttonAction: {
      type: Function,
      default: () => {}
    },
    autoHide: {
      type: Number,
      default: null
    },
    show: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isVisible: this.show,
      timeLeft: 0,
      timer: null,
      progress: 100,
      isTimerActive: false
    }
  },
  computed: {
    formattedTime() {
      const seconds = Math.ceil(this.timeLeft / 1000)
      return `${seconds}с`
    },
    shouldShowTimer() {
      return this.autoHide !== null && this.autoHide > 0
    }
  },
  watch: {
    show(newVal) {
      this.isVisible = newVal
      if (newVal && this.shouldShowTimer) {
        this.startTimer()
      }
    }
  },
  methods: {
    handleButtonClick() {
      this.buttonAction()
      this.hidePanel()
    },

    showPanel() {
      this.isVisible = true
    },

    startCountdown() {
      if (this.shouldShowTimer) {
        this.startTimer()
      }
    },

    hidePanel() {
      this.isVisible = false
      this.stopTimer()
      this.$emit('hidden')
    },

    startTimer() {
      this.stopTimer()

      this.timeLeft = this.autoHide
      this.progress = 100
      this.isTimerActive = true

      const startTime = Date.now()
      const interval = 50

      this.timer = setInterval(() => {
        const elapsed = Date.now() - startTime
        this.timeLeft = Math.max(0, this.autoHide - elapsed)
        this.progress = Math.max(0, (this.timeLeft / this.autoHide) * 100)

        if (this.timeLeft <= 0) {
          this.hidePanel()
        }
      }, interval)
    },

    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
        this.isTimerActive = false
      }
    },

    pauseTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    resumeTimer() {
      if (this.isTimerActive && this.timeLeft > 0) {
        this.startTimerFromCurrentTime()
      }
    },

    startTimerFromCurrentTime() {
      this.stopTimer()

      const remainingTime = this.timeLeft
      const startTime = Date.now() - (this.autoHide - remainingTime)
      const interval = 50

      this.timer = setInterval(() => {
        const elapsed = Date.now() - startTime
        this.timeLeft = Math.max(0, this.autoHide - elapsed)
        this.progress = Math.max(0, (this.timeLeft / this.autoHide) * 100)

        if (this.timeLeft <= 0) {
          this.hidePanel()
        }
      }, interval)
    }
  },
  mounted() {
    if (this.isVisible && this.shouldShowTimer) {
      this.startTimer()
    }
  },
  beforeUnmount() {
    this.stopTimer()
  }
}
</script>

<template>
  <transition name="push-panel">
    <div v-if="isVisible" class="push-panel">
      <div class="push-panel__content">
        <div class="push-panel__message">{{ message }}</div>
        <div class="push-panel__timer" v-if="shouldShowTimer">
          <div class="timer-circle">
            <svg class="timer-svg" width="40" height="40" viewBox="0 0 40 40">
              <circle
                cx="20"
                cy="20"
                r="18"
                stroke="#e0e0e0"
                stroke-width="2"
                fill="none"
              />
              <circle
                cx="20"
                cy="20"
                r="18"
                stroke="#369b70"
                stroke-width="2"
                fill="none"
                :stroke-dasharray="`${2 * Math.PI * 18}`"
                :stroke-dashoffset="`${2 * Math.PI * 18 * (1 - progress / 100)}`"
                stroke-linecap="round"
                transform="rotate(-90 20 20)"
                class="timer-progress"
              />
            </svg>
            <div class="timer-text">{{ formattedTime }}</div>
          </div>
        </div>

        <button
          class="push-panel__button"
          @click="handleButtonClick"
        >
          {{ buttonText }}
        </button>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.push-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  max-width: 90%;
}

.push-panel__content {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.push-panel__message {
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
  color: #333;
}

.push-panel__timer {
  display: flex;
  align-items: center;
}

.timer-circle {
  position: relative;
  width: 40px;
  height: 40px;
}

.timer-svg {
  width: 40px;
  height: 40px;
}

.timer-progress {
  transition: stroke-dashoffset 0.1s linear;
}

.timer-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 600;
  color: #369b70;
}

.push-panel__button {
  background: #369b70;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.push-panel__button:hover {
  background: #2a7a58;
}

.push-panel-enter-active,
.push-panel-leave-active {
  transition: all 0.3s ease;
}

.push-panel-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}

.push-panel-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}
</style>
