# Docker Setup Guide - ConversationalAI4J

## 🚀 Quick Start (Complete Working System)

With Docker Compose, you can get a complete working ConversationalAI system in one command:

```bash
git clone https://github.com/SchulteDev/ConversationalAI4J.git
cd ConversationalAI4J
docker-compose up
```

**That's it!** 🎉 The system will:
- ✅ Pull Ollama AI service
- ✅ Download llama3.2:1b model automatically  
- ✅ Build and start the demo application
- ✅ Set up proper networking and health checks

## 🌐 Access Your AI System

Once all services are up and healthy (takes ~2-3 minutes):

- **Demo Web Interface**: http://localhost:8080
- **Ollama API**: http://localhost:11434

## 📋 Service Overview

### 🤖 Ollama Service
- **Purpose**: AI model hosting and processing
- **Port**: 11434
- **Model**: llama3.2:1b (1.2B parameters, ~1.3GB)
- **Health Check**: `/bin/ollama list`

### 🎯 Demo Application  
- **Purpose**: Spring Boot web interface for conversations
- **Port**: 8080  
- **Framework**: Spring Boot 3.5.4 with Thymeleaf
- **Health Check**: `/actuator/health`

### 🔧 Ollama-Init Service
- **Purpose**: One-time model download and setup
- **Behavior**: Pulls llama3.2:1b model then exits
- **Dependencies**: Waits for Ollama to be healthy

## 🛠️ Development Commands

### Start Services
```bash
# Start all services in background
docker-compose up -d

# Start with logs visible
docker-compose up

# Rebuild and start (after code changes)
docker-compose up --build
```

### Service Management
```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs demo
docker-compose logs ollama

# Stop services
docker-compose down

# Complete cleanup (removes volumes)
docker-compose down -v
```

### Testing
```bash
# Test demo application
curl http://localhost:8080

# Test Ollama API
curl http://localhost:11434/api/tags

# Send AI message via web interface
curl -X POST -d "message=Hello!" http://localhost:8080/send
```

## 📊 System Requirements

- **Docker**: Version 20.10+ with Compose V2
- **Memory**: 4GB+ recommended (Ollama model loading)
- **Storage**: ~2GB for model and images
- **Platform**: Windows, macOS, Linux

## 🔍 Architecture Benefits

### Library Module (Pure API)
- ✅ **No Infrastructure Dependencies**: Just client-side integration code
- ✅ **Framework Agnostic**: Works with any Java application
- ✅ **Simple Integration**: `ConversationalAI.builder().withOllamaModel("llama3.2:1b").build()`

### Demo Module (Complete Infrastructure)
- ✅ **Production Example**: Shows real-world deployment with Docker
- ✅ **Easy Setup**: `docker-compose up` provides fully working system
- ✅ **Best Practices**: Health checks, proper networking, containerization

## 🚨 Troubleshooting

### Service Won't Start
```bash
# Check logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]

# Force rebuild
docker-compose up --build --force-recreate
```

### Model Download Issues
```bash
# Check ollama-init logs
docker-compose logs ollama-init

# Manually pull model
docker exec conversationalai4j-ollama ollama pull llama3.2:1b
```

### Port Conflicts
```bash
# Stop conflicting services
docker ps
docker stop [container-id]

# Or modify ports in docker-compose.yml
ports:
  - "8081:8080"  # Change host port
```

## 🎯 Next Steps

1. **Try the Demo**: Visit http://localhost:8080 and chat with the AI
2. **Explore the API**: Check the ConversationalAI library integration  
3. **Customize**: Modify docker-compose.yml for your needs
4. **Scale**: Add multiple demo instances or different models

---

**🎉 Congratulations!** You now have a complete, production-ready conversational AI system running with Docker!