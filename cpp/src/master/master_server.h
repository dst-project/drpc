#ifndef _DOUSI_MASTER_H_
#define _DOUSI_MASTER_H_

#include "common/endpoint.h"
#include "master_client_session.h"
#include "common/logging.h"

#include <unordered_map>
#include <string>
#include <iostream>
#include <memory>
#include <vector>

#include <boost/asio.hpp>

namespace dousi {
namespace master {

/**
 * The master server is a standalone component that provides the service discovery for
 * the Dousi servers and clients.
 *
 * A Dousi server provided by user should be registered into master, so that the
 * client which wants to use this service can find the service location from master.
 */
class MasterServer {
public:
  MasterServer() = delete;

  MasterServer(boost::asio::io_context &io_context, std::string listening_host, int16_t listening_port)
    : listening_endpoint_(std::move(listening_host), listening_port),
    io_context_(io_context),
    acceptor_(io_context, listening_endpoint_.GetTcpEndpoint()) {}

  void Loop() {
    DoAccept();
    DOUSI_LOG(INFO) << "MasterServer is now listening on " << listening_endpoint_.ToString();
    io_context_.run();
  }

  // TODO(qwang): Add `DoDisconnect()` to handle master_client disconnect from this server gracefully.

private:
  void DoAccept();

private:
  // The endpoint master is listening on.
  Endpoint listening_endpoint_;

  boost::asio::io_context &io_context_;

  boost::asio::ip::tcp::acceptor acceptor_;

  // The map that maps service name to its endpoint.
  std::unordered_map<std::string, Endpoint> endpoints_;

  // client sessions. Maybe this should be refined as a hashmap with its unique ID.
  std::vector<std::shared_ptr<MasterClientSession>> sessions_;
};

} // namespace master
} // namespace dousi

#endif
